package com.tyler.sentinel.repositoryimport.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class FileTreeScannerServiceTests {

    @Test
    void scansExtensionlessReadmeFiles() throws Exception {
        Path repositoryRoot = Files.createTempDirectory("sentinel-scan-test-");
        try {
            Path readme = repositoryRoot.resolve("README");
            Files.writeString(readme, "hello world");

            FileTreeScannerService scanner = new FileTreeScannerService(
                    new FileMetadataService(),
                    2097152,
                    ".git,node_modules,target,build,dist,out,.idea,.vscode,__pycache__,.venv,vendor"
            );

            var files = scanner.scan(repositoryRoot);

            assertThat(files).hasSize(1);
            assertThat(files.get(0).fileName()).isEqualTo("README");
            assertThat(files.get(0).fileType()).isEqualTo("DOCUMENTATION");
        } finally {
            try (var paths = Files.walk(repositoryRoot)) {
                paths.sorted((first, second) -> second.compareTo(first))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                            }
                        });
            }
        }
    }

    @Test
    void scansRepositoryClonedByGitCloneService() {
        GitCloneService gitCloneService = new GitCloneService("./tmp/test-repos");
        FileTreeScannerService scanner = new FileTreeScannerService(
                new FileMetadataService(),
                2097152,
                ".git,node_modules,target,build,dist,out,.idea,.vscode,__pycache__,.venv,vendor"
        );
        ClonedRepository clonedRepository = null;

        try {
            GitRepositoryInfo repositoryInfo = gitCloneService.parseRepositoryInfo("https://github.com/spring-guides/gs-rest-service");
            clonedRepository = gitCloneService.cloneRepository(repositoryInfo);
            var files = scanner.scan(clonedRepository.directory());

            assertThat(files).isNotEmpty();
        } finally {
            if (clonedRepository != null) {
                gitCloneService.deleteClone(clonedRepository.directory());
            }
        }
    }
}
