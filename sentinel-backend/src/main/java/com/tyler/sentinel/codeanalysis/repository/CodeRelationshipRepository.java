package com.tyler.sentinel.codeanalysis.repository;

import com.tyler.sentinel.codeanalysis.entity.CodeRelationship;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CodeRelationshipRepository extends JpaRepository<CodeRelationship, Long> {

    List<CodeRelationship> findByProjectIdOrderBySourceSymbolNameAscTargetSymbolNameAsc(Long projectId);

    @Query("""
            select relationship from CodeRelationship relationship
            where relationship.project.user.id = :userId
              and (:projectId is null or relationship.project.id = :projectId)
              and (
                lower(relationship.relationshipType) like lower(concat('%', :query, '%'))
                or lower(relationship.sourceSymbol.name) like lower(concat('%', :query, '%'))
                or lower(relationship.targetSymbol.name) like lower(concat('%', :query, '%'))
                or lower(relationship.sourceSymbol.file.filePath) like lower(concat('%', :query, '%'))
                or lower(relationship.targetSymbol.file.filePath) like lower(concat('%', :query, '%'))
              )
            order by relationship.sourceSymbol.name asc, relationship.targetSymbol.name asc
            """)
    List<CodeRelationship> searchForUser(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("query") String query,
            Pageable pageable
    );

    long countByProjectId(Long projectId);

    @Transactional
    void deleteByProjectId(Long projectId);
}
