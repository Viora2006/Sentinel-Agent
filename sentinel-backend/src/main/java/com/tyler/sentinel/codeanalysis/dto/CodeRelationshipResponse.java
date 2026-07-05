package com.tyler.sentinel.codeanalysis.dto;

public record CodeRelationshipResponse(
        Long relationshipId,
        String relationshipType,
        Long sourceSymbolId,
        String sourceName,
        String sourceType,
        Long targetSymbolId,
        String targetName,
        String targetType
) {
}
