package com.tyler.sentinel.codeanalysis.entity;

import com.tyler.sentinel.repositoryimport.entity.Project;
import com.tyler.sentinel.repositoryimport.entity.ProjectFile;
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
@Table(name = "code_symbols")
public class CodeSymbol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private ProjectFile file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_symbol_id")
    private CodeSymbol parentSymbol;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(length = 2048)
    private String signature;

    private Integer startLine;

    private Integer endLine;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CodeSymbol() {
    }

    public CodeSymbol(
            Project project,
            ProjectFile file,
            CodeSymbol parentSymbol,
            String type,
            String name,
            String signature,
            Integer startLine,
            Integer endLine
    ) {
        this.project = project;
        this.file = file;
        this.parentSymbol = parentSymbol;
        this.type = type;
        this.name = name;
        this.signature = signature;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public ProjectFile getFile() {
        return file;
    }

    public CodeSymbol getParentSymbol() {
        return parentSymbol;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getSignature() {
        return signature;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }
}
