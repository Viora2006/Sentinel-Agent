package com.tyler.sentinel.repositoryimport.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileTreeScannerService {

    private final FileMetadataService fileMetadataService;
    private final long maxFileSizeBytes;
    private final Set<String> ignoredDirectories;

    public FileTreeScannerService(
            FileMetadataService fileMetadataService,
            @Value("${sentinel.repo.max-file-size-bytes:2097152}") long maxFileSizeBytes,
            @Value("${sentinel.repo.ignored-directories:.git,node_modules,target,build,dist,out,.idea,.vscode,__pycache__,.venv,vendor}") String ignoredDirectories
    ) {
        this.fileMetadataService = fileMetadataService;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.ignoredDirectories = new HashSet<>(Arrays.stream(ignoredDirectories.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList());
    }

    public List<FileMetadata> scan(Path repoRoot) {
        try (Stream<Path> paths = Files.walk(repoRoot)) {
            List<FileMetadata> metadata = new ArrayList<>();
            paths.forEach(path -> {
                if (Files.isRegularFile(path) && !isIgnored(repoRoot, path) && isWithinMaxSize(path)) {
                    metadata.add(fileMetadataService.buildMetadata(repoRoot, path));
                }
            });
            return metadata;
        } catch (IOException exception) {
            throw new RepositoryImportException("Unable to scan repository file tree.", exception);
        }
    }

    private boolean isIgnored(Path repoRoot, Path path) {
        Path relativePath = repoRoot.relativize(path);
        for (Path segment : relativePath) {
            if (ignoredDirectories.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean isWithinMaxSize(Path path) {
        try {
            return Files.size(path) <= maxFileSizeBytes;
        } catch (IOException exception) {
            return false;
        }
    }
}
