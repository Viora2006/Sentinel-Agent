package com.tyler.sentinel.security;

import com.tyler.sentinel.model.UserSession;
import com.tyler.sentinel.repository.UserSessionRepository;
import com.tyler.sentinel.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserSessionRepository userSessionRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserSessionRepository userSessionRepository) {
        this.jwtService = jwtService;
        this.userSessionRepository = userSessionRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorizationHeader.substring(7);
            Claims claims = jwtService.parseClaims(token);

            if (isActiveSession(claims)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean isActiveSession(Claims claims) {
        return userSessionRepository.findByTokenId(claims.getId())
                .filter(session -> !session.isRevoked())
                .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
                .map(UserSession::getUser)
                .map(user -> user.getUsername().equals(claims.getSubject()))
                .orElse(false);
    }
}
