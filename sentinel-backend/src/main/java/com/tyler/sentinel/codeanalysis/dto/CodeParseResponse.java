package com.tyler.sentinel.codeanalysis.dto;

public record CodeParseResponse(
        Long projectId,
        int parsedFiles,
        int skippedFiles,
        int symbolsCreated,
        int relationshipsCreated,
        String message
) {
}
