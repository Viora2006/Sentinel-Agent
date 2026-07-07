package com.tyler.sentinel.codesearch.service;

import com.tyler.sentinel.codeanalysis.entity.CodeRelationship;
import com.tyler.sentinel.codeanalysis.entity.CodeSymbol;
import com.tyler.sentinel.codeanalysis.repository.CodeRelationshipRepository;
import com.tyler.sentinel.codeanalysis.repository.CodeSymbolRepository;
import com.tyler.sentinel.codesearch.dto.CodeSearchResponse;
import com.tyler.sentinel.codesearch.dto.CodeSearchResult;
import com.tyler.sentinel.model.User;
import com.tyler.sentinel.repository.UserRepository;
import com.tyler.sentinel.repositoryimport.entity.Project;
import com.tyler.sentinel.repositoryimport.entity.ProjectFile;
import com.tyler.sentinel.repositoryimport.repository.ProjectFileRepository;
import com.tyler.sentinel.repositoryimport.repository.ProjectRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CodeSearchService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_QUERY_LENGTH = 120;

    private final CodeRelationshipRepository codeRelationshipRepository;
    private final CodeSymbolRepository codeSymbolRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public CodeSearchService(
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
    }

    @Transactional(readOnly = true)
    public CodeSearchResponse search(String rawQuery, Long projectId, Integer rawLimit) {
        User user = currentUser();
        String query = normalizeQuery(rawQuery);
        int limit = normalizeLimit(rawLimit);

        if (projectId != null) {
            projectRepository.findByIdAndUserId(projectId, user.getId())
                    .orElseThrow(() -> new CodeSearchException("Project was not found."));
        }

        PageRequest page = PageRequest.of(0, limit);
        List<CodeSearchResult> results = new ArrayList<>();

        projectFileRepository.searchForUser(user.getId(), projectId, query, page).stream()
                .map(file -> toFileResult(file, query))
                .forEach(results::add);

        codeSymbolRepository.searchForUser(user.getId(), projectId, query, page).stream()
                .map(symbol -> toSymbolResult(symbol, query))
                .forEach(results::add);

        codeRelationshipRepository.searchForUser(user.getId(), projectId, query, page).stream()
                .map(relationship -> toRelationshipResult(relationship, query))
                .forEach(results::add);

        List<CodeSearchResult> rankedResults = results.stream()
                .sorted(Comparator.comparingInt(CodeSearchResult::score).reversed()
                        .thenComparing(CodeSearchResult::filePath, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(limit)
                .toList();

        return new CodeSearchResponse(query, projectId, rankedResults.size(), rankedResults);
    }

    private CodeSearchResult toFileResult(ProjectFile file, String query) {
        Project project = file.getProject();
        return new CodeSearchResult(
                "FILE",
                project.getId(),
                project.getName(),
                file.getId(),
                file.getFilePath(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                scoreFile(file, query),
                preview(file.getContent(), query, "Matched file path or stored source content.")
        );
    }

    private CodeSearchResult toSymbolResult(CodeSymbol symbol, String query) {
        Project project = symbol.getProject();
        ProjectFile file = symbol.getFile();
        return new CodeSearchResult(
                "SYMBOL",
                project.getId(),
                project.getName(),
                file.getId(),
                file.getFilePath(),
                symbol.getId(),
                symbol.getType(),
                symbol.getName(),
                null,
                null,
                null,
                null,
                symbol.getStartLine(),
                symbol.getEndLine(),
                scoreSymbol(symbol, query),
                preview(symbol.getSignature(), query, symbol.getType() + " " + symbol.getName())
        );
    }

    private CodeSearchResult toRelationshipResult(CodeRelationship relationship, String query) {
        Project project = relationship.getProject();
        CodeSymbol source = relationship.getSourceSymbol();
        CodeSymbol target = relationship.getTargetSymbol();
        ProjectFile file = source.getFile();
        return new CodeSearchResult(
                "RELATIONSHIP",
                project.getId(),
                project.getName(),
                file.getId(),
                file.getFilePath(),
                source.getId(),
                source.getType(),
                source.getName(),
                relationship.getId(),
                relationship.getRelationshipType(),
                source.getName(),
                target.getName(),
                source.getStartLine(),
                source.getEndLine(),
                scoreRelationship(relationship, query),
                source.getName() + " " + relationship.getRelationshipType() + " " + target.getName()
        );
    }

    private int scoreFile(ProjectFile file, String query) {
        int score = 10;
        score += scoreText(file.getFilePath(), query, 30, 12);
        score += scoreText(file.getFileName(), query, 35, 15);
        score += scoreText(file.getLanguage(), query, 10, 5);
        score += scoreText(file.getContent(), query, 18, 4);
        return score;
    }

    private int scoreSymbol(CodeSymbol symbol, String query) {
        int score = 25;
        score += scoreText(symbol.getName(), query, 45, 18);
        score += scoreText(symbol.getType(), query, 20, 8);
        score += scoreText(symbol.getSignature(), query, 24, 8);
        score += scoreText(symbol.getFile().getFilePath(), query, 14, 5);
        return score;
    }

    private int scoreRelationship(CodeRelationship relationship, String query) {
        int score = 30;
        score += scoreText(relationship.getRelationshipType(), query, 35, 14);
        score += scoreText(relationship.getSourceSymbol().getName(), query, 32, 12);
        score += scoreText(relationship.getTargetSymbol().getName(), query, 32, 12);
        score += scoreText(relationship.getSourceSymbol().getFile().getFilePath(), query, 10, 4);
        return score;
    }

    private int scoreText(String value, String query, int exactScore, int containsScore) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        String normalizedValue = value.toLowerCase(Locale.ROOT);
        String normalizedQuery = query.toLowerCase(Locale.ROOT);

        if (normalizedValue.equals(normalizedQuery)) {
            return exactScore;
        }
        if (normalizedValue.contains(normalizedQuery)) {
            return containsScore;
        }
        return 0;
    }

    private String preview(String value, String query, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String normalizedValue = value.toLowerCase(Locale.ROOT);
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        int index = normalizedValue.indexOf(normalizedQuery);

        if (index < 0) {
            return trimPreview(value);
        }

        int start = Math.max(0, index - 90);
        int end = Math.min(value.length(), index + query.length() + 160);
        String prefix = start > 0 ? "... " : "";
        String suffix = end < value.length() ? " ..." : "";
        return prefix + value.substring(start, end).replaceAll("\\s+", " ").trim() + suffix;
    }

    private String trimPreview(String value) {
        String compact = value.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 260) {
            return compact;
        }
        return compact.substring(0, 260).trim() + " ...";
    }

    private String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new CodeSearchException("Search query is required.");
        }

        String query = rawQuery.trim();
        if (query.length() < 2) {
            throw new CodeSearchException("Search query must be at least 2 characters.");
        }
        if (query.length() > MAX_QUERY_LENGTH) {
            throw new CodeSearchException("Search query must be 120 characters or fewer.");
        }
        return query;
    }

    private int normalizeLimit(Integer rawLimit) {
        if (rawLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(rawLimit, MAX_LIMIT));
    }

    private User currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new CodeSearchException("You must be signed in to search code.");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new CodeSearchException("Signed-in user could not be found."));
    }
}
