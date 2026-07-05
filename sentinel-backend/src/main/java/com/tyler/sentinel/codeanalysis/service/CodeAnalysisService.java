package com.tyler.sentinel.codeanalysis.service;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.tyler.sentinel.codeanalysis.dto.CodeParseResponse;
import com.tyler.sentinel.codeanalysis.dto.CodeRelationshipResponse;
import com.tyler.sentinel.codeanalysis.dto.CodeSymbolResponse;
import com.tyler.sentinel.codeanalysis.entity.CodeRelationship;
import com.tyler.sentinel.codeanalysis.entity.CodeSymbol;
import com.tyler.sentinel.codeanalysis.repository.CodeRelationshipRepository;
import com.tyler.sentinel.codeanalysis.repository.CodeSymbolRepository;
import com.tyler.sentinel.model.User;
import com.tyler.sentinel.repository.UserRepository;
import com.tyler.sentinel.repositoryimport.entity.Project;
import com.tyler.sentinel.repositoryimport.entity.ProjectFile;
import com.tyler.sentinel.repositoryimport.repository.ProjectFileRepository;
import com.tyler.sentinel.repositoryimport.repository.ProjectRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CodeAnalysisService {

    private final CodeRelationshipRepository codeRelationshipRepository;
    private final CodeSymbolRepository codeSymbolRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public CodeAnalysisService(
            CodeRelationshipRepository codeRelationshipRepository,
            CodeSymbolRepository codeSymbolRepository,
            ProjectFileRepository projectFileRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository
    ) {
        this.codeRelationshipRepository = codeRelationshipRepository;
        this.codeSymbolRepository = codeSymbolRepository;
        this.projectFileRepository = projectFileRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        StaticJavaParser.setConfiguration(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));
    }

    @Transactional
    public CodeParseResponse parseProject(Long projectId) {
        Project project = currentUserProject(projectId);
        List<ProjectFile> javaFiles = projectFileRepository.findByProjectIdOrderByFilePathAsc(project.getId()).stream()
                .filter(file -> "Java".equalsIgnoreCase(file.getLanguage()))
                .filter(file -> file.getContent() != null && !file.getContent().isBlank())
                .toList();

        codeRelationshipRepository.deleteByProjectId(project.getId());
        codeSymbolRepository.deleteByProjectId(project.getId());

        int parsedFiles = 0;
        int skippedFiles = 0;
        List<CodeSymbol> symbols = new ArrayList<>();
        List<CodeRelationship> relationships = new ArrayList<>();

        for (ProjectFile file : javaFiles) {
            try {
                ParseContext context = parseJavaFile(project, file);
                symbols.addAll(context.symbols());
                relationships.addAll(context.relationships());
                parsedFiles++;
            } catch (RuntimeException exception) {
                skippedFiles++;
            }
        }

        return new CodeParseResponse(
                project.getId(),
                parsedFiles,
                skippedFiles,
                Math.toIntExact(codeSymbolRepository.countByProjectId(project.getId())),
                Math.toIntExact(codeRelationshipRepository.countByProjectId(project.getId())),
                "Code parsing completed."
        );
    }

    @Transactional(readOnly = true)
    public List<CodeSymbolResponse> listSymbols(Long projectId) {
        Project project = currentUserProject(projectId);
        return codeSymbolRepository.findByProjectIdOrderByFileFilePathAscStartLineAsc(project.getId()).stream()
                .map(this::toSymbolResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CodeRelationshipResponse> listRelationships(Long projectId) {
        Project project = currentUserProject(projectId);
        return codeRelationshipRepository.findByProjectIdOrderBySourceSymbolNameAscTargetSymbolNameAsc(project.getId()).stream()
                .map(this::toRelationshipResponse)
                .toList();
    }

    private ParseContext parseJavaFile(Project project, ProjectFile file) {
        CompilationUnit compilationUnit = StaticJavaParser.parse(file.getContent());

        List<CodeSymbol> symbols = new ArrayList<>();
        List<CodeRelationship> relationships = new ArrayList<>();
        Map<Node, CodeSymbol> ownerSymbols = new LinkedHashMap<>();

        CodeSymbol packageSymbol = compilationUnit.getPackageDeclaration()
                .map(packageDeclaration -> {
                    CodeSymbol symbol = saveSymbol(project, file, null, "PACKAGE", packageDeclaration.getNameAsString(),
                            packageDeclaration.getNameAsString(), packageDeclaration, symbols);
                    ownerSymbols.put(packageDeclaration, symbol);
                    return symbol;
                })
                .orElse(null);

        List<Node> topLevelOwners = new ArrayList<>();
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(this::isTopLevelType)
                .forEach(type -> topLevelOwners.add(type));
        compilationUnit.findAll(EnumDeclaration.class).stream()
                .filter(this::isTopLevelType)
                .forEach(type -> topLevelOwners.add(type));
        compilationUnit.findAll(RecordDeclaration.class).stream()
                .filter(this::isTopLevelType)
                .forEach(type -> topLevelOwners.add(type));

        for (Node node : topLevelOwners) {
            CodeSymbol typeSymbol = createTypeSymbol(project, file, packageSymbol, node, symbols, relationships);
            ownerSymbols.put(node, typeSymbol);
        }

        compilationUnit.findAll(ImportDeclaration.class).forEach(importDeclaration -> {
            CodeSymbol importSymbol = saveSymbol(project, file, null, "IMPORT", importDeclaration.getNameAsString(),
                    importDeclaration.toString().trim(), importDeclaration, symbols);
            CodeSymbol source = firstOwner(ownerSymbols, packageSymbol);
            if (source != null) {
                relationships.add(saveRelationship(project, source, importSymbol, "IMPORTS"));
            }
        });

        for (ClassOrInterfaceDeclaration type : compilationUnit.findAll(ClassOrInterfaceDeclaration.class)) {
            CodeSymbol owner = ownerFor(type, ownerSymbols);
            if (owner == null) {
                owner = createTypeSymbol(project, file, parentSymbol(type, ownerSymbols, packageSymbol), type, symbols, relationships);
                ownerSymbols.put(type, owner);
            }
            captureMembers(project, file, type, owner, symbols, relationships);
        }

        for (EnumDeclaration type : compilationUnit.findAll(EnumDeclaration.class)) {
            CodeSymbol owner = ownerFor(type, ownerSymbols);
            if (owner == null) {
                owner = createTypeSymbol(project, file, parentSymbol(type, ownerSymbols, packageSymbol), type, symbols, relationships);
                ownerSymbols.put(type, owner);
            }
            captureMembers(project, file, type, owner, symbols, relationships);
        }

        for (RecordDeclaration type : compilationUnit.findAll(RecordDeclaration.class)) {
            CodeSymbol owner = ownerFor(type, ownerSymbols);
            if (owner == null) {
                owner = createTypeSymbol(project, file, parentSymbol(type, ownerSymbols, packageSymbol), type, symbols, relationships);
                ownerSymbols.put(type, owner);
            }
            captureMembers(project, file, type, owner, symbols, relationships);
        }

        return new ParseContext(symbols, relationships);
    }

    private void captureMembers(
            Project project,
            ProjectFile file,
            Node type,
            CodeSymbol owner,
            List<CodeSymbol> symbols,
            List<CodeRelationship> relationships
    ) {
        type.findAll(FieldDeclaration.class).stream()
                .filter(field -> belongsDirectlyTo(field, type))
                .forEach(field -> {
                    for (VariableDeclarator variable : field.getVariables()) {
                        CodeSymbol symbol = saveSymbol(project, file, owner, "FIELD", variable.getNameAsString(),
                                field.getElementType().asString() + " " + variable.getNameAsString(), variable, symbols);
                        relationships.add(saveRelationship(project, owner, symbol, "CONTAINS"));
                    }
                });

        type.findAll(ConstructorDeclaration.class).stream()
                .filter(constructor -> belongsDirectlyTo(constructor, type))
                .forEach(constructor -> {
                    CodeSymbol symbol = saveSymbol(project, file, owner, "CONSTRUCTOR", constructor.getNameAsString(),
                            constructor.getDeclarationAsString(false, false, false), constructor, symbols);
                    relationships.add(saveRelationship(project, owner, symbol, "CONTAINS"));
                });

        type.findAll(MethodDeclaration.class).stream()
                .filter(method -> belongsDirectlyTo(method, type))
                .forEach(method -> {
                    CodeSymbol symbol = saveSymbol(project, file, owner, "METHOD", method.getNameAsString(),
                            method.getDeclarationAsString(false, false, false), method, symbols);
                    relationships.add(saveRelationship(project, owner, symbol, "CONTAINS"));
                });

        type.findAll(AnnotationExpr.class).stream()
                .filter(annotation -> belongsDirectlyTo(annotation, type))
                .forEach(annotation -> {
                    CodeSymbol symbol = saveSymbol(project, file, owner, "ANNOTATION", annotation.getNameAsString(),
                            annotation.toString(), annotation, symbols);
                    relationships.add(saveRelationship(project, owner, symbol, "ANNOTATED_BY"));
                });
    }

    private CodeSymbol createTypeSymbol(
            Project project,
            ProjectFile file,
            CodeSymbol parent,
            Node node,
            List<CodeSymbol> symbols,
            List<CodeRelationship> relationships
    ) {
        String type = typeName(node);
        String name = node instanceof NodeWithSimpleName<?> namedNode ? namedNode.getNameAsString() : "anonymous";
        CodeSymbol symbol = saveSymbol(project, file, parent, type, name, name, node, symbols);
        if (parent != null) {
            relationships.add(saveRelationship(project, parent, symbol, "CONTAINS"));
        }
        return symbol;
    }

    private CodeSymbol saveSymbol(
            Project project,
            ProjectFile file,
            CodeSymbol parent,
            String type,
            String name,
            String signature,
            Node node,
            List<CodeSymbol> symbols
    ) {
        CodeSymbol symbol = codeSymbolRepository.save(new CodeSymbol(
                project,
                file,
                parent,
                type,
                name,
                signature,
                startLine(node),
                endLine(node)
        ));
        symbols.add(symbol);
        return symbol;
    }

    private CodeRelationship saveRelationship(Project project, CodeSymbol source, CodeSymbol target, String type) {
        return codeRelationshipRepository.save(new CodeRelationship(project, source, target, type));
    }

    private CodeSymbol ownerFor(Node node, Map<Node, CodeSymbol> ownerSymbols) {
        return ownerSymbols.get(node);
    }

    private CodeSymbol parentSymbol(Node node, Map<Node, CodeSymbol> ownerSymbols, CodeSymbol fallback) {
        return node.getParentNode()
                .flatMap(parent -> parent.findAncestor(Node.class, ownerSymbols::containsKey))
                .map(ownerSymbols::get)
                .orElse(fallback);
    }

    private CodeSymbol firstOwner(Map<Node, CodeSymbol> ownerSymbols, CodeSymbol fallback) {
        return ownerSymbols.values().stream().findFirst().orElse(fallback);
    }

    private boolean isTopLevelType(Node node) {
        return node.getParentNode().filter(parent -> parent instanceof CompilationUnit).isPresent();
    }

    private boolean belongsDirectlyTo(Node member, Node owner) {
        return nearestTypeAncestor(member)
                .map(node -> node == owner)
                .orElse(false);
    }

    private Optional<Node> nearestTypeAncestor(Node node) {
        Optional<Node> current = node.getParentNode();
        while (current.isPresent()) {
            Node parent = current.get();
            if (parent instanceof ClassOrInterfaceDeclaration
                    || parent instanceof EnumDeclaration
                    || parent instanceof RecordDeclaration) {
                return Optional.of(parent);
            }
            current = parent.getParentNode();
        }
        return Optional.empty();
    }

    private String typeName(Node node) {
        if (node instanceof ClassOrInterfaceDeclaration declaration) {
            return declaration.isInterface() ? "INTERFACE" : "CLASS";
        }
        if (node instanceof EnumDeclaration) {
            return "ENUM";
        }
        if (node instanceof RecordDeclaration) {
            return "RECORD";
        }
        return "TYPE";
    }

    private Integer startLine(Node node) {
        return node.getRange().map(range -> range.begin.line).orElse(null);
    }

    private Integer endLine(Node node) {
        return node.getRange().map(range -> range.end.line).orElse(null);
    }

    private CodeSymbolResponse toSymbolResponse(CodeSymbol symbol) {
        return new CodeSymbolResponse(
                symbol.getId(),
                symbol.getFile().getId(),
                symbol.getParentSymbol() == null ? null : symbol.getParentSymbol().getId(),
                symbol.getFile().getFilePath(),
                symbol.getType(),
                symbol.getName(),
                symbol.getSignature(),
                symbol.getStartLine(),
                symbol.getEndLine()
        );
    }

    private CodeRelationshipResponse toRelationshipResponse(CodeRelationship relationship) {
        CodeSymbol source = relationship.getSourceSymbol();
        CodeSymbol target = relationship.getTargetSymbol();
        return new CodeRelationshipResponse(
                relationship.getId(),
                relationship.getRelationshipType(),
                source.getId(),
                source.getName(),
                source.getType(),
                target.getId(),
                target.getName(),
                target.getType()
        );
    }

    private Project currentUserProject(Long projectId) {
        User user = currentUser();
        return projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new CodeAnalysisException("Project was not found."));
    }

    private User currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new CodeAnalysisException("You must be signed in to analyze code.");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new CodeAnalysisException("Signed-in user could not be found."));
    }

    private record ParseContext(List<CodeSymbol> symbols, List<CodeRelationship> relationships) {
    }
}
