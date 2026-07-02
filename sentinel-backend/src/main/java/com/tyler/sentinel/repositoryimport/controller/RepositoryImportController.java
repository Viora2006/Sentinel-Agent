package com.tyler.sentinel.repositoryimport.controller;

import com.tyler.sentinel.repositoryimport.dto.ProjectFileResponse;
import com.tyler.sentinel.repositoryimport.dto.ProjectFileSummaryResponse;
import com.tyler.sentinel.repositoryimport.dto.ProjectResponse;
import com.tyler.sentinel.repositoryimport.dto.RepositoryImportErrorResponse;
import com.tyler.sentinel.repositoryimport.dto.RepositoryImportRequest;
import com.tyler.sentinel.repositoryimport.dto.RepositoryImportResponse;
import com.tyler.sentinel.repositoryimport.service.DuplicateProjectException;
import com.tyler.sentinel.repositoryimport.service.RepositoryImportException;
import com.tyler.sentinel.repositoryimport.service.RepositoryImportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class RepositoryImportController {

    private final RepositoryImportService repositoryImportService;

    public RepositoryImportController(RepositoryImportService repositoryImportService) {
        this.repositoryImportService = repositoryImportService;
    }

    @GetMapping
    public List<ProjectResponse> listProjects() {
        return repositoryImportService.listProjectsForCurrentUser();
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getProject(@PathVariable Long projectId) {
        return repositoryImportService.getProjectForCurrentUser(projectId);
    }

    @GetMapping("/{projectId}/files")
    public List<ProjectFileSummaryResponse> listProjectFiles(@PathVariable Long projectId) {
        return repositoryImportService.listProjectFilesForCurrentUser(projectId);
    }

    @GetMapping("/{projectId}/files/{fileId}")
    public ProjectFileResponse getProjectFile(@PathVariable Long projectId, @PathVariable Long fileId) {
        return repositoryImportService.getProjectFileForCurrentUser(projectId, fileId);
    }

    @PostMapping("/import")
    public RepositoryImportResponse importRepository(@Valid @RequestBody RepositoryImportRequest request) {
        return repositoryImportService.importRepository(
                request.getGithubUrl(),
                request.getProjectName(),
                request.getDescription(),
                request.isUpdateExisting()
        );
    }

    @ExceptionHandler(DuplicateProjectException.class)
    public ResponseEntity<RepositoryImportErrorResponse> handleDuplicateProjectException(DuplicateProjectException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new RepositoryImportErrorResponse(
                exception.getMessage(),
                "DUPLICATE_PROJECT",
                exception.getExistingProjectId(),
                exception.getExistingProjectName()
        ));
    }

    @ExceptionHandler(RepositoryImportException.class)
    public ResponseEntity<RepositoryImportErrorResponse> handleRepositoryImportException(RepositoryImportException exception) {
        return ResponseEntity.badRequest().body(new RepositoryImportErrorResponse(
                exception.getMessage(),
                "REPOSITORY_IMPORT_ERROR",
                null,
                null
        ));
    }
}
