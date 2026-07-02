package com.tyler.sentinel.repositoryimport.util;

import java.util.Map;

public final class LanguageDetector {

    private static final Map<String, String> LANGUAGES = Map.ofEntries(
            Map.entry("java", "Java"),
            Map.entry("js", "JavaScript"),
            Map.entry("jsx", "JavaScript/React"),
            Map.entry("ts", "TypeScript"),
            Map.entry("tsx", "TypeScript/React"),
            Map.entry("py", "Python"),
            Map.entry("cpp", "C++"),
            Map.entry("c", "C"),
            Map.entry("h", "C/C++ Header"),
            Map.entry("cs", "C#"),
            Map.entry("go", "Go"),
            Map.entry("rs", "Rust"),
            Map.entry("rb", "Ruby"),
            Map.entry("php", "PHP"),
            Map.entry("html", "HTML"),
            Map.entry("css", "CSS"),
            Map.entry("xml", "XML"),
            Map.entry("json", "JSON"),
            Map.entry("yml", "YAML"),
            Map.entry("yaml", "YAML"),
            Map.entry("properties", "Properties"),
            Map.entry("md", "Markdown")
    );

    private LanguageDetector() {
    }

    public static String detect(String extension) {
        if (extension == null || extension.isBlank()) {
            return "Unknown";
        }

        return LANGUAGES.getOrDefault(extension.toLowerCase(), "Unknown");
    }
}
