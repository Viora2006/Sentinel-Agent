package com.tyler.sentinel.repositoryimport.dto;

public record RepositoryImportErrorResponse(
        String message,
        String code,
        Long existingProjectId,
        String existingProjectName
) {
}
