package com.tyler.sentinel.codesearch.dto;

public record CodeSearchResult(
        String resultType,
        Long projectId,
        String projectName,
        Long fileId,
        String filePath,
        Long symbolId,
        String symbolType,
        String symbolName,
        Long relationshipId,
        String relationshipType,
        String sourceName,
        String targetName,
        Integer startLine,
        Integer endLine,
        int score,
        String preview
) {
}
