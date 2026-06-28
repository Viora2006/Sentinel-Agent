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
    public AuthResponse register(Credentials credentials) {
        String username = normalizeUsername(credentials.getUsername());

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken.");
        }

        User user = userRepository.save(new User(username, passwordEncoder.encode(credentials.getPassword())));
        JwtService.GeneratedToken generatedToken = jwtService.generateToken(user);
        userSessionRepository.save(new UserSession(generatedToken.tokenId(), user, generatedToken.expiresAt()));

        return new AuthResponse("Registration successful.", generatedToken.token(), user.getId(), user.getUsername());
    }

    @Transactional
    public AuthResponse login(Credentials credentials) {
        String username = normalizeUsername(credentials.getUsername());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password."));

        if (!passwordEncoder.matches(credentials.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password.");
        }

        JwtService.GeneratedToken generatedToken = jwtService.generateToken(user);
        userSessionRepository.save(new UserSession(generatedToken.tokenId(), user, generatedToken.expiresAt()));

        return new AuthResponse("Login successful.", generatedToken.token(), user.getId(), user.getUsername());
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
}
