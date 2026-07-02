package com.tyler.sentinel.repositoryimport.repository;

import com.tyler.sentinel.repositoryimport.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Project> findByIdAndUserId(Long id, Long userId);

    Optional<Project> findByUserIdAndGithubUrl(Long userId, String githubUrl);
}
