package com.tyler.sentinel.controller;

import com.tyler.sentinel.dto.AuthResponse;
import com.tyler.sentinel.dto.Credentials;
import com.tyler.sentinel.dto.MessageResponse;
import com.tyler.sentinel.service.AuthService;
import com.tyler.sentinel.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody Credentials credentials) {
        return authService.register(credentials);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody Credentials credentials) {
        return authService.login(credentials);
    }

    @PostMapping("/logout")
    public MessageResponse logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");
        Claims claims = jwtService.parseClaims(token);
        authService.revokeToken(claims.getId());
        return new MessageResponse("Logout successful.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new MessageResponse(exception.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(401).body(new MessageResponse(exception.getMessage()));
    }
}
