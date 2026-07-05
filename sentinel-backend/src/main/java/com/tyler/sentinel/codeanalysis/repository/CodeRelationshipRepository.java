package com.tyler.sentinel.codeanalysis.repository;

import com.tyler.sentinel.codeanalysis.entity.CodeRelationship;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface CodeRelationshipRepository extends JpaRepository<CodeRelationship, Long> {

    List<CodeRelationship> findByProjectIdOrderBySourceSymbolNameAscTargetSymbolNameAsc(Long projectId);

    long countByProjectId(Long projectId);

    @Transactional
    void deleteByProjectId(Long projectId);
}
