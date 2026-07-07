package com.tyler.sentinel.codeanalysis.repository;

import com.tyler.sentinel.codeanalysis.entity.CodeSymbol;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CodeSymbolRepository extends JpaRepository<CodeSymbol, Long> {

    List<CodeSymbol> findByProjectIdOrderByFileFilePathAscStartLineAsc(Long projectId);

    @Query("""
            select symbol from CodeSymbol symbol
            where symbol.project.user.id = :userId
              and (:projectId is null or symbol.project.id = :projectId)
              and (
                lower(symbol.name) like lower(concat('%', :query, '%'))
                or lower(symbol.type) like lower(concat('%', :query, '%'))
                or lower(coalesce(symbol.signature, '')) like lower(concat('%', :query, '%'))
                or lower(symbol.file.filePath) like lower(concat('%', :query, '%'))
              )
            order by symbol.file.filePath asc, symbol.startLine asc
            """)
    List<CodeSymbol> searchForUser(
            @Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("query") String query,
            Pageable pageable
    );

    long countByProjectId(Long projectId);

    @Transactional
    void deleteByProjectId(Long projectId);
}
