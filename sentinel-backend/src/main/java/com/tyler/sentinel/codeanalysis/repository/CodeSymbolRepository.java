package com.tyler.sentinel.codeanalysis.repository;

import com.tyler.sentinel.codeanalysis.entity.CodeSymbol;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface CodeSymbolRepository extends JpaRepository<CodeSymbol, Long> {

    List<CodeSymbol> findByProjectIdOrderByFileFilePathAscStartLineAsc(Long projectId);

    long countByProjectId(Long projectId);

    @Transactional
    void deleteByProjectId(Long projectId);
}
