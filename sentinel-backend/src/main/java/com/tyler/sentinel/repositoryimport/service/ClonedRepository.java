package com.tyler.sentinel.repositoryimport.service;

import java.nio.file.Path;

public record ClonedRepository(Path directory, String defaultBranch) {
}
