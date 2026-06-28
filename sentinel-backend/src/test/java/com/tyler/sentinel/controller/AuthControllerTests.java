package com.tyler.sentinel.controller;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tyler.sentinel.model.User;
import com.tyler.sentinel.repository.UserRepository;
import com.tyler.sentinel.repository.UserSessionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    private String createdUsername;

    @AfterEach
    void cleanUp() {
        if (createdUsername == null) {
            return;
        }

        Optional<User> user = userRepository.findByUsername(createdUsername);
        user.ifPresent(value -> {
            userSessionRepository.deleteByUser(value);
            userRepository.delete(value);
        });
    }

    @Test
    void registerCreatesUserAndReturnsJwt() throws Exception {
        createdUsername = "register_" + UUID.randomUUID().toString().replace("-", "");
        String body = """
                {
                    "username": "%s",
                    "password": "test-password-123"
                }
                """.formatted(createdUsername);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful."))
                .andExpect(jsonPath("$.token").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.username").value(createdUsername));
    }
}
