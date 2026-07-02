package com.tyler.sentinel.repositoryimport.dto;

public record ProjectFileSummaryResponse(
        Long fileId,
        String filePath,
        String fileName,
        String extension,
        String language,
        String fileType,
        Long sizeBytes,
        String contentHash
) {
}
