package com.tyler.sentinel.service;

import com.tyler.sentinel.dto.AuthResponse;
import com.tyler.sentinel.dto.Credentials;
import com.tyler.sentinel.model.User;
import com.tyler.sentinel.model.UserSession;
import com.tyler.sentinel.repository.UserRepository;
import com.tyler.sentinel.repository.UserSessionRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;

    public AuthService(
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            UserSessionRepository userSessionRepository
    ) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
    }

    @Transactional
    public AuthResult register(Credentials credentials) {
        String username = normalizeUsername(credentials.getUsername());

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken.");
        }

        User user = userRepository.save(new User(username, passwordEncoder.encode(credentials.getPassword())));
        JwtService.GeneratedToken generatedToken = jwtService.generateToken(user);
        userSessionRepository.save(new UserSession(generatedToken.tokenId(), user, generatedToken.expiresAt()));

        return new AuthResult(
                new AuthResponse("Registration successful.", user.getId(), user.getUsername()),
                generatedToken.token(),
                generatedToken.tokenId(),
                generatedToken.expiresAt()
        );
    }

    @Transactional
    public AuthResult login(Credentials credentials) {
        String username = normalizeUsername(credentials.getUsername());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password."));

        if (!passwordEncoder.matches(credentials.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password.");
        }

        JwtService.GeneratedToken generatedToken = jwtService.generateToken(user);
        userSessionRepository.save(new UserSession(generatedToken.tokenId(), user, generatedToken.expiresAt()));

        return new AuthResult(
                new AuthResponse("Login successful.", user.getId(), user.getUsername()),
                generatedToken.token(),
                generatedToken.tokenId(),
                generatedToken.expiresAt()
        );
    }

    @Transactional
    public void revokeToken(String tokenId) {
        userSessionRepository.findByTokenId(tokenId).ifPresent(session -> {
            session.revoke();
            userSessionRepository.save(session);
        });
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }

    public record AuthResult(AuthResponse response, String token, String tokenId, java.time.Instant expiresAt) {
    }
}
