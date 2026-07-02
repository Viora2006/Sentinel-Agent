package com.tyler.sentinel.repositoryimport.service;

import com.tyler.sentinel.repositoryimport.util.FileTypeDetector;
import com.tyler.sentinel.repositoryimport.util.HashUtil;
import com.tyler.sentinel.repositoryimport.util.LanguageDetector;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

@Service
public class FileMetadataService {

    public FileMetadata buildMetadata(Path repoRoot, Path file) {
        Path relativePath = repoRoot.relativize(file);
        String normalizedPath = relativePath.toString().replace('\\', '/');
        String fileName = file.getFileName().toString();
        String extension = extension(fileName);
        String language = LanguageDetector.detect(extension);
        String fileType = FileTypeDetector.detect(normalizedPath, fileName, extension);
        long sizeBytes = file.toFile().length();
        String contentHash = HashUtil.sha256(file);
        String content = readContent(file, fileType);

        return new FileMetadata(normalizedPath, fileName, extension, language, fileType, sizeBytes, contentHash, content);
    }

    private String extension(String fileName) {
        int extensionStart = fileName.lastIndexOf('.');
        if (extensionStart < 0 || extensionStart == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(extensionStart + 1).toLowerCase();
    }

    private String readContent(Path file, String fileType) {
        if ("BINARY".equals(fileType)) {
            return null;
        }

        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }
}
