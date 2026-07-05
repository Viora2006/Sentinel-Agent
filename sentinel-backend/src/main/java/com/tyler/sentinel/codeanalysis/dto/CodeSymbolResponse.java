package com.tyler.sentinel.codeanalysis.dto;

public record CodeSymbolResponse(
        Long symbolId,
        Long fileId,
        Long parentSymbolId,
        String filePath,
        String type,
        String name,
        String signature,
        Integer startLine,
        Integer endLine
) {
}
