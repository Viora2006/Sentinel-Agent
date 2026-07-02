package com.tyler.sentinel.repositoryimport.service;

public class DuplicateProjectException extends RepositoryImportException {

    private final Long existingProjectId;
    private final String existingProjectName;

    public DuplicateProjectException(Long existingProjectId, String existingProjectName) {
        super("This GitHub repository is already imported as " + existingProjectName + ".");
        this.existingProjectId = existingProjectId;
        this.existingProjectName = existingProjectName;
    }

    public Long getExistingProjectId() {
        return existingProjectId;
    }

    public String getExistingProjectName() {
        return existingProjectName;
    }
}
