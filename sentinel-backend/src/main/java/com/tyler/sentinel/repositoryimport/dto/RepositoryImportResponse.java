package com.tyler.sentinel.repositoryimport.dto;

import java.util.Map;

public record RepositoryImportResponse(
        Long projectId,
        String projectName,
        String repoName,
        String githubUrl,
        String description,
        String status,
        int totalFiles,
        Map<String, Integer> languages,
        String message
) {
}
