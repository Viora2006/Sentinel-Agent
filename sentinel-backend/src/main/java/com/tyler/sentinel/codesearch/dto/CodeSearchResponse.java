package com.tyler.sentinel.codesearch.dto;

import java.util.List;

public record CodeSearchResponse(
        String query,
        Long projectId,
        int resultCount,
        List<CodeSearchResult> results
) {
}
