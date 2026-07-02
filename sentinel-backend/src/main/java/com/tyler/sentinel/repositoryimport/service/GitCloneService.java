package com.tyler.sentinel.repositoryimport.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GitCloneService {

    private static final Duration CLONE_TIMEOUT = Duration.ofMinutes(3);

    private final Path tempDirectory;

    public GitCloneService(@Value("${sentinel.repo.temp-dir:./tmp/repos}") String tempDirectory) {
        this.tempDirectory = Path.of(tempDirectory).toAbsolutePath().normalize();
    }

    public GitRepositoryInfo parseRepositoryInfo(String githubUrl) {
        if (githubUrl == null || githubUrl.isBlank()) {
            throw new RepositoryImportException("GitHub URL is required.");
        }

        try {
            URI uri = new URI(githubUrl.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !"github.com".equalsIgnoreCase(uri.getHost())) {
                throw new RepositoryImportException("Only HTTPS GitHub URLs are supported.");
            }

            String[] pathParts = uri.getPath().replaceFirst("^/", "").split("/");
            if (pathParts.length < 2 || pathParts[0].isBlank() || pathParts[1].isBlank()) {
                throw new RepositoryImportException("GitHub URL must include an owner and repository name.");
            }

            String ownerName = pathParts[0];
            String repoName = pathParts[1].replaceFirst("\\.git$", "");
            String normalizedUrl = "https://github.com/%s/%s".formatted(ownerName, repoName);

            return new GitRepositoryInfo(normalizedUrl, ownerName, repoName);
        } catch (URISyntaxException exception) {
            throw new RepositoryImportException("Invalid GitHub URL.", exception);
        }
    }

    public ClonedRepository cloneRepository(GitRepositoryInfo repositoryInfo) {
        try {
            Files.createDirectories(tempDirectory);
            Path cloneRoot = Files.createTempDirectory(tempDirectory, repositoryInfo.repoName() + "-");
            runCommand(List.of("git", "clone", "--depth", "1", repositoryInfo.githubUrl(), cloneRoot.toString()), tempDirectory);
            String defaultBranch = readDefaultBranch(cloneRoot);
            return new ClonedRepository(cloneRoot, defaultBranch);
        } catch (IOException exception) {
            throw new RepositoryImportException("Unable to create temporary clone directory.", exception);
        }
    }

    public void deleteClone(Path cloneRoot) {
        if (cloneRoot == null || !Files.exists(cloneRoot)) {
            return;
        }

        try (var paths = Files.walk(cloneRoot)) {
            paths.sorted((first, second) -> second.compareTo(first))
                    .forEach(path -> {
                        try {
                            path.toFile().setWritable(true);
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            System.err.println("Unable to delete temporary repository file: " + path);
                        }
                    });
        } catch (IOException exception) {
            System.err.println("Unable to clean up temporary clone directory: " + cloneRoot);
        }
    }

    private String readDefaultBranch(Path cloneRoot) {
        try {
            return runCommand(List.of("git", "-C", cloneRoot.toString(), "rev-parse", "--abbrev-ref", "HEAD"), cloneRoot).trim();
        } catch (RepositoryImportException exception) {
            return null;
        }
    }

    private String runCommand(List<String> command, Path workingDirectory) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(workingDirectory.toFile());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            boolean finished = process.waitFor(CLONE_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes());

            if (!finished) {
                process.destroyForcibly();
                throw new RepositoryImportException("Git command timed out.");
            }

            if (process.exitValue() != 0) {
                throw new RepositoryImportException("Git command failed: " + output.strip());
            }

            return output;
        } catch (IOException exception) {
            throw new RepositoryImportException("Unable to run git. Make sure Git is installed and on PATH.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RepositoryImportException("Git command was interrupted.", exception);
        }
    }
}
