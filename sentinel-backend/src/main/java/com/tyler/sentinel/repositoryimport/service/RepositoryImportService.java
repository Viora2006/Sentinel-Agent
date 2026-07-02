package com.tyler.sentinel.repositoryimport.service;

import com.tyler.sentinel.model.User;
import com.tyler.sentinel.repository.UserRepository;
import com.tyler.sentinel.repositoryimport.dto.ProjectFileResponse;
import com.tyler.sentinel.repositoryimport.dto.ProjectFileSummaryResponse;
import com.tyler.sentinel.repositoryimport.dto.ProjectResponse;
import com.tyler.sentinel.repositoryimport.dto.RepositoryImportResponse;
import com.tyler.sentinel.repositoryimport.entity.Project;
import com.tyler.sentinel.repositoryimport.entity.ProjectFile;
import com.tyler.sentinel.repositoryimport.repository.ProjectFileRepository;
import com.tyler.sentinel.repositoryimport.repository.ProjectRepository;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryImportService {

    private final FileTreeScannerService fileTreeScannerService;
    private final GitCloneService gitCloneService;
    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final UserRepository userRepository;

    public RepositoryImportService(
            FileTreeScannerService fileTreeScannerService,
            GitCloneService gitCloneService,
            ProjectRepository projectRepository,
            ProjectFileRepository projectFileRepository,
            UserRepository userRepository
    ) {
        this.fileTreeScannerService = fileTreeScannerService;
        this.gitCloneService = gitCloneService;
        this.projectRepository = projectRepository;
        this.projectFileRepository = projectFileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjectsForCurrentUser() {
        User user = currentUser();
        return projectRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toProjectResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectForCurrentUser(Long projectId) {
        User user = currentUser();
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new RepositoryImportException("Project was not found."));
        return toProjectResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectFileSummaryResponse> listProjectFilesForCurrentUser(Long projectId) {
        User user = currentUser();
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new RepositoryImportException("Project was not found."));

        return projectFileRepository.findByProjectIdOrderByFilePathAsc(project.getId()).stream()
                .map(this::toFileSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectFileResponse getProjectFileForCurrentUser(Long projectId, Long fileId) {
        User user = currentUser();
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new RepositoryImportException("Project was not found."));
        ProjectFile file = projectFileRepository.findByIdAndProjectId(fileId, project.getId())
                .orElseThrow(() -> new RepositoryImportException("Project file was not found."));
        return toFileResponse(file);
    }

    @Transactional
    public RepositoryImportResponse importRepository(String githubUrl, String projectName, String description, boolean updateExisting) {
        User user = currentUser();
        GitRepositoryInfo repositoryInfo = gitCloneService.parseRepositoryInfo(githubUrl);
        Project project = resolveImportProject(user, repositoryInfo, projectName, description, updateExisting);
        Path cloneRoot = null;

        try {
            ClonedRepository clonedRepository = gitCloneService.cloneRepository(repositoryInfo);
            cloneRoot = clonedRepository.directory();

            List<FileMetadata> files = fileTreeScannerService.scan(cloneRoot);
            List<ProjectFile> projectFiles = files.stream()
                    .map(file -> toProjectFile(project, file))
                    .toList();

            projectFileRepository.deleteByProjectId(project.getId());
            projectFileRepository.saveAll(projectFiles);
            project.setDefaultBranch(clonedRepository.defaultBranch());
            project.setTotalFiles(projectFiles.size());
            project.setTotalSizeBytes(files.stream().mapToLong(FileMetadata::sizeBytes).sum());
            project.setLastImportedAt(LocalDateTime.now());
            project.setStatus("IMPORTED");
            project.setErrorMessage(null);
            projectRepository.save(project);

            return new RepositoryImportResponse(
                    project.getId(),
                    project.getName(),
                    project.getRepoName(),
                    project.getGithubUrl(),
                    project.getDescription(),
                    project.getStatus(),
                    project.getTotalFiles(),
                    languageCounts(files),
                    "Repository imported successfully"
            );
        } catch (RuntimeException exception) {
            markFailed(project, exception);
            throw exception;
        } finally {
            gitCloneService.deleteClone(cloneRoot);
        }
    }

    @Transactional
    protected Project resolveImportProject(
            User user,
            GitRepositoryInfo repositoryInfo,
            String projectName,
            String description,
            boolean updateExisting
    ) {
        var existingProject = projectRepository.findByUserIdAndGithubUrl(user.getId(), repositoryInfo.githubUrl());

        if (existingProject.isPresent() && !updateExisting) {
            Project project = existingProject.get();
            throw new DuplicateProjectException(project.getId(), project.getName());
        }

        if (existingProject.isPresent()) {
            Project project = existingProject.get();
            project.setName(projectName.trim());
            project.setDescription(normalizeOptional(description));
            project.setStatus("IMPORTING");
            project.setErrorMessage(null);
            return projectRepository.save(project);
        }

        Project project = new Project(
                user,
                projectName.trim(),
                normalizeOptional(description),
                repositoryInfo.githubUrl(),
                repositoryInfo.repoName(),
                repositoryInfo.ownerName(),
                "IMPORTING"
        );
        return projectRepository.save(project);
    }

    @Transactional
    protected void markFailed(Project project, RuntimeException exception) {
        project.setStatus("FAILED");
        project.setErrorMessage(exception.getMessage());
        projectRepository.save(project);
    }

    private ProjectFile toProjectFile(Project project, FileMetadata file) {
        return new ProjectFile(
                project,
                file.filePath(),
                file.fileName(),
                file.extension(),
                file.language(),
                file.fileType(),
                file.sizeBytes(),
                file.contentHash(),
                file.content()
        );
    }

    private Map<String, Integer> languageCounts(List<FileMetadata> files) {
        return files.stream()
                .collect(Collectors.groupingBy(FileMetadata::language, LinkedHashMap::new, Collectors.summingInt(file -> 1)));
    }

    private ProjectResponse toProjectResponse(Project project) {
        Map<String, Integer> languages = projectFileRepository.findByProjectIdOrderByFilePathAsc(project.getId()).stream()
                .collect(Collectors.groupingBy(ProjectFile::getLanguage, LinkedHashMap::new, Collectors.summingInt(file -> 1)));

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getRepoName(),
                project.getGithubUrl(),
                project.getStatus(),
                project.getTotalFiles(),
                languages,
                project.getCreatedAt(),
                project.getLastImportedAt()
        );
    }

    private ProjectFileSummaryResponse toFileSummaryResponse(ProjectFile file) {
        return new ProjectFileSummaryResponse(
                file.getId(),
                file.getFilePath(),
                file.getFileName(),
                file.getExtension(),
                file.getLanguage(),
                file.getFileType(),
                file.getSizeBytes(),
                file.getContentHash()
        );
    }

    private ProjectFileResponse toFileResponse(ProjectFile file) {
        return new ProjectFileResponse(
                file.getId(),
                file.getProject().getId(),
                file.getFilePath(),
                file.getFileName(),
                file.getExtension(),
                file.getLanguage(),
                file.getFileType(),
                file.getSizeBytes(),
                file.getContentHash(),
                file.getContent()
        );
    }

    private User currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new RepositoryImportException("You must be signed in to access projects.");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RepositoryImportException("Signed-in user could not be found."));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }
}
