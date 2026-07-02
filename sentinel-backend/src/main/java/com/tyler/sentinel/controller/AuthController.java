package com.tyler.sentinel.controller;

import com.tyler.sentinel.dto.AuthResponse;
import com.tyler.sentinel.dto.Credentials;
import com.tyler.sentinel.dto.MessageResponse;
import com.tyler.sentinel.model.User;
import com.tyler.sentinel.repository.UserRepository;
import com.tyler.sentinel.service.AuthService;
import com.tyler.sentinel.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final String authCookieName;
    private final boolean secureCookie;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            UserRepository userRepository,
            @Value("${app.auth.cookie-name:sentinel_session}") String authCookieName,
            @Value("${app.auth.cookie-secure:false}") boolean secureCookie
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authCookieName = authCookieName;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody Credentials credentials) {
        AuthService.AuthResult result = authService.register(credentials);
        return withAuthCookie(result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody Credentials credentials) {
        AuthService.AuthResult result = authService.login(credentials);
        return withAuthCookie(result);
    }

    @GetMapping("/me")
    public AuthResponse me() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw new BadCredentialsException("Not signed in.");
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Not signed in."));
        return new AuthResponse("Signed in.", user.getId(), user.getUsername());
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        String token = tokenFromCookie(request);
        if (token == null || token.isBlank()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, expiredAuthCookie().toString())
                    .body(new MessageResponse("Logout successful."));
        }

        try {
            Claims claims = jwtService.parseClaims(token);
            authService.revokeToken(claims.getId());
        } catch (RuntimeException ignored) {
            // Logout should still clear a stale or malformed browser cookie.
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredAuthCookie().toString())
                .body(new MessageResponse("Logout successful."));
    }

    private ResponseEntity<AuthResponse> withAuthCookie(AuthService.AuthResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie(result.token(), result.expiresAt()).toString())
                .body(result.response());
    }

    private ResponseCookie authCookie(String token, Instant expiresAt) {
        long maxAgeSeconds = Math.max(0, Duration.between(Instant.now(), expiresAt).toSeconds());
        return ResponseCookie.from(authCookieName, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private ResponseCookie expiredAuthCookie() {
        return ResponseCookie.from(authCookieName, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private String tokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (authCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new MessageResponse(exception.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(401).body(new MessageResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Invalid request.");
        return ResponseEntity.badRequest().body(new MessageResponse(message));
    }

    @ExceptionHandler({ RuntimeException.class, AuthenticationException.class })
    public ResponseEntity<MessageResponse> handleAuthenticationFailure(RuntimeException exception) {
        return ResponseEntity.status(401).body(new MessageResponse("Authentication failed."));
    }
}
