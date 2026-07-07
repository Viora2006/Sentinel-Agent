package com.tyler.sentinel.repositoryimport.repository;

import com.tyler.sentinel.repositoryimport.entity.ProjectFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {

    List<ProjectFile> findByProjectIdOrderByFilePathAsc(Long projectId);

    Optional<ProjectFile> findByIdAndProjectId(Long id, Long projectId);

    @Query("""
            select file from ProjectFile file
            where file.project.user.id = :userId
              and (:projectId is null or file.project.id = :projectId)
              and (
                lower(file.filePath) like lower(concat('%', :query, '%'))
                or lower(file.fileName) like lower(concat('%', :query, '%'))
                or lower(file.language) like lower(concat('%', :query, '%'))
                or lower(coalesce(file.content, '')) like lower(concat('%', :query, '%'))
              )
            order by file.filePath asc
            """)
    List<ProjectFile> searchForUser(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("query") String query,
            Pageable pageable
    );

    @Transactional
    void deleteByProjectId(Long projectId);
}
