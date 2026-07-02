package com.tyler.sentinel.repositoryimport.dto;

public record ProjectFileResponse(
        Long fileId,
        Long projectId,
        String filePath,
        String fileName,
        String extension,
        String language,
        String fileType,
        Long sizeBytes,
        String contentHash,
        String content
) {
}
