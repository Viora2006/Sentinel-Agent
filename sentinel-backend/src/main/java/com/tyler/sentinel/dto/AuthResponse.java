package com.tyler.sentinel.dto;

public record AuthResponse(String message, String token, Long userId, String username) {
}
