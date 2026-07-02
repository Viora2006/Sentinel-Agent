package com.tyler.sentinel.repositoryimport.util;

import java.util.Set;

public final class FileTypeDetector {

    private static final Set<String> SOURCE_EXTENSIONS = Set.of(
            "java", "js", "jsx", "ts", "tsx", "py", "cpp", "c", "h", "cs", "go", "rs", "rb", "php", "html", "css"
    );

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "ico", "pdf", "zip", "tar", "gz", "jar", "class", "exe", "dll", "so", "dylib"
    );

    private static final Set<String> CONFIG_FILES = Set.of(
            "pom.xml", "package.json", "application.properties", "application.yml", "application.yaml", "dockerfile",
            ".env.example", "tsconfig.json", "vite.config.js", "vite.config.ts"
    );

    private FileTypeDetector() {
    }

    public static String detect(String relativePath, String fileName, String extension) {
        String normalizedPath = relativePath.replace('\\', '/').toLowerCase();
        String normalizedName = fileName.toLowerCase();
        String normalizedExtension = extension == null ? "" : extension.toLowerCase();

        if (normalizedPath.contains("/test/") || normalizedPath.contains("/tests/")
                || normalizedName.contains("spec.") || normalizedName.endsWith("test.java")
                || normalizedName.contains(".test.") || normalizedName.contains(".spec.")) {
            return "TEST";
        }

        if (BINARY_EXTENSIONS.contains(normalizedExtension)) {
            return "BINARY";
        }

        if (normalizedName.equals("readme") || normalizedName.equals("readme.md")
                || normalizedExtension.equals("md") || normalizedPath.contains("/docs/")) {
            return "DOCUMENTATION";
        }

        if (CONFIG_FILES.contains(normalizedName) || normalizedName.startsWith("gradle")
                || normalizedExtension.equals("gradle") || normalizedPath.contains("/build.gradle")) {
            return "CONFIG";
        }

        if (normalizedName.contains("mvnw") || normalizedName.equals("makefile") || normalizedExtension.equals("lock")) {
            return "BUILD";
        }

        if (SOURCE_EXTENSIONS.contains(normalizedExtension)) {
            return "SOURCE";
        }

        return "UNKNOWN";
    }
}
