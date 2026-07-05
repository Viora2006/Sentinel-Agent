package com.tyler.sentinel.codeanalysis.service;

public class CodeAnalysisException extends RuntimeException {

    public CodeAnalysisException(String message) {
        super(message);
    }

    public CodeAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
