package com.tyler.sentinel.repositoryimport.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ProjectResponse(
        Long projectId,
        String projectName,
        String description,
        String repoName,
        String githubUrl,
        String status,
        int totalFiles,
        Map<String, Integer> languages,
        LocalDateTime createdAt,
        LocalDateTime lastImportedAt
) {
}
