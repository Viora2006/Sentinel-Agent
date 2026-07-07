package com.tyler.sentinel.codesearch.controller;

import com.tyler.sentinel.codesearch.dto.CodeSearchResponse;
import com.tyler.sentinel.codesearch.service.CodeSearchException;
import com.tyler.sentinel.codesearch.service.CodeSearchService;
import com.tyler.sentinel.dto.MessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/code-search")
public class CodeSearchController {

    private final CodeSearchService codeSearchService;

    public CodeSearchController(CodeSearchService codeSearchService) {
        this.codeSearchService = codeSearchService;
    }

    @GetMapping
    public CodeSearchResponse search(
            @RequestParam("query") String query,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return codeSearchService.search(query, projectId, limit);
    }

    @ExceptionHandler(CodeSearchException.class)
    public ResponseEntity<MessageResponse> handleCodeSearchException(CodeSearchException exception) {
        return ResponseEntity.badRequest().body(new MessageResponse(exception.getMessage()));
    }
}
