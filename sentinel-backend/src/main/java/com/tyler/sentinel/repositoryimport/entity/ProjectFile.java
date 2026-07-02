package com.tyler.sentinel.repositoryimport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_files")
public class ProjectFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 2048)
    private String filePath;

    @Column(nullable = false, length = 512)
    private String fileName;

    @Column(length = 100)
    private String extension;

    @Column(nullable = false, length = 100)
    private String language;

    @Column(nullable = false, length = 50)
    private String fileType;

    @Column(nullable = false)
    private Long sizeBytes;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @Column(columnDefinition = "text")
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ProjectFile() {
    }

    public ProjectFile(
            Project project,
            String filePath,
            String fileName,
            String extension,
            String language,
            String fileType,
            Long sizeBytes,
            String contentHash,
            String content
    ) {
        this.project = project;
        this.filePath = filePath;
        this.fileName = fileName;
        this.extension = extension;
        this.language = language;
        this.fileType = fileType;
        this.sizeBytes = sizeBytes;
        this.contentHash = contentHash;
        this.content = content;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public String getLanguage() {
        return language;
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getExtension() {
        return extension;
    }

    public String getFileType() {
        return fileType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getContent() {
        return content;
    }
}
