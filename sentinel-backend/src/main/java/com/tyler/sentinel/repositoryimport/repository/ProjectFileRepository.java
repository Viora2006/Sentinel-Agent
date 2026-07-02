package com.tyler.sentinel.repositoryimport.repository;

import com.tyler.sentinel.repositoryimport.entity.ProjectFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {

    List<ProjectFile> findByProjectIdOrderByFilePathAsc(Long projectId);

    Optional<ProjectFile> findByIdAndProjectId(Long id, Long projectId);

    @Transactional
    void deleteByProjectId(Long projectId);
}
