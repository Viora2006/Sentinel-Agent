package com.tyler.sentinel.codeanalysis.controller;

import com.tyler.sentinel.codeanalysis.dto.CodeParseResponse;
import com.tyler.sentinel.codeanalysis.dto.CodeRelationshipResponse;
import com.tyler.sentinel.codeanalysis.dto.CodeSymbolResponse;
import com.tyler.sentinel.codeanalysis.service.CodeAnalysisException;
import com.tyler.sentinel.codeanalysis.service.CodeAnalysisService;
import com.tyler.sentinel.dto.MessageResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/code")
public class CodeAnalysisController {

    private final CodeAnalysisService codeAnalysisService;

    public CodeAnalysisController(CodeAnalysisService codeAnalysisService) {
        this.codeAnalysisService = codeAnalysisService;
    }

    @PostMapping("/parse")
    public CodeParseResponse parseProject(@PathVariable Long projectId) {
        return codeAnalysisService.parseProject(projectId);
    }

    @GetMapping("/symbols")
    public List<CodeSymbolResponse> listSymbols(@PathVariable Long projectId) {
        return codeAnalysisService.listSymbols(projectId);
    }

    @GetMapping("/relationships")
    public List<CodeRelationshipResponse> listRelationships(@PathVariable Long projectId) {
        return codeAnalysisService.listRelationships(projectId);
    }

    @ExceptionHandler(CodeAnalysisException.class)
    public ResponseEntity<MessageResponse> handleCodeAnalysisException(CodeAnalysisException exception) {
        return ResponseEntity.badRequest().body(new MessageResponse(exception.getMessage()));
    }
}
