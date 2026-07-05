package com.tyler.sentinel.codeanalysis.entity;

import com.tyler.sentinel.repositoryimport.entity.Project;
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
@Table(name = "code_relationships")
public class CodeRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_symbol_id", nullable = false)
    private CodeSymbol sourceSymbol;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_symbol_id", nullable = false)
    private CodeSymbol targetSymbol;

    @Column(nullable = false, length = 100)
    private String relationshipType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CodeRelationship() {
    }

    public CodeRelationship(Project project, CodeSymbol sourceSymbol, CodeSymbol targetSymbol, String relationshipType) {
        this.project = project;
        this.sourceSymbol = sourceSymbol;
        this.targetSymbol = targetSymbol;
        this.relationshipType = relationshipType;
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

    public CodeSymbol getSourceSymbol() {
        return sourceSymbol;
    }

    public CodeSymbol getTargetSymbol() {
        return targetSymbol;
    }

    public String getRelationshipType() {
        return relationshipType;
    }
}
