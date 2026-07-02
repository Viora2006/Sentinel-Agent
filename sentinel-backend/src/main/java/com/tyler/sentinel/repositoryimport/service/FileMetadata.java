package com.tyler.sentinel.repositoryimport.service;

public record FileMetadata(
        String filePath,
        String fileName,
        String extension,
        String language,
        String fileType,
        long sizeBytes,
        String contentHash,
        String content
) {
}
