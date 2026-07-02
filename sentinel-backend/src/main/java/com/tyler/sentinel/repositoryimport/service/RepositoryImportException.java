package com.tyler.sentinel.repositoryimport.service;

public class RepositoryImportException extends RuntimeException {

    public RepositoryImportException(String message) {
        super(message);
    }

    public RepositoryImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
