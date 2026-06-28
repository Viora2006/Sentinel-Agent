package com.tyler.sentinel.repository;

import com.tyler.sentinel.model.UserSession;
import com.tyler.sentinel.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByTokenId(String tokenId);

    @Modifying
    @Transactional
    void deleteByUser(User user);
}
