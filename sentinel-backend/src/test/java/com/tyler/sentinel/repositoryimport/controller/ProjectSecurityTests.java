package com.tyler.sentinel.repositoryimport.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import com.tyler.sentinel.model.User;
import com.tyler.sentinel.repository.UserRepository;
import com.tyler.sentinel.repository.UserSessionRepository;
import com.tyler.sentinel.repositoryimport.entity.Project;
import com.tyler.sentinel.repositoryimport.entity.ProjectFile;
import com.tyler.sentinel.repositoryimport.repository.ProjectFileRepository;
import com.tyler.sentinel.repositoryimport.repository.ProjectRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectFileRepository projectFileRepository;

    private final List<String> createdUsernames = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String username : createdUsernames) {
            userRepository.findByUsername(username).ifPresent(user -> {
                projectRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).forEach(projectRepository::delete);
                userSessionRepository.deleteByUser(user);
                userRepository.delete(user);
            });
        }
    }

    @Test
    void projectEndpointsDoNotExposeOtherUsersData() throws Exception {
        User owner = createUser("owner");
        User otherUser = createUser("other");
        Cookie otherUserCookie = login(otherUser.getUsername());
        Project project = projectRepository.save(new Project(
                owner,
                "Owner Project",
                "Private project",
                "https://github.com/example/private-project",
                "private-project",
                "example",
                "IMPORTED"
        ));
        ProjectFile file = projectFileRepository.save(new ProjectFile(
                project,
                "src/App.java",
                "App.java",
                "java",
                "Java",
                "SOURCE",
                12L,
                "0".repeat(64),
                "class App {}"
        ));

        mockMvc.perform(get("/api/projects/" + project.getId())
                        .cookie(otherUserCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Project was not found."));

        mockMvc.perform(get("/api/projects/" + project.getId() + "/files")
                        .cookie(otherUserCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Project was not found."));

        mockMvc.perform(get("/api/projects/" + project.getId() + "/files/" + file.getId())
                        .cookie(otherUserCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Project was not found."));
    }

    @Test
    void logoutRevokesJwtSession() throws Exception {
        String username = createUser("logout").getUsername();
        Cookie cookie = login(username);

        mockMvc.perform(get("/api/projects")
                        .cookie(cookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(cookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/projects")
                        .cookie(cookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerRejectsWeakPassword() throws Exception {
        String username = "weak_" + UUID.randomUUID().toString().replace("-", "");
        String body = """
                {
                    "username": "%s",
                    "password": "short"
                }
                """.formatted(username);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private User createUser(String prefix) {
        String username = prefix + "_" + UUID.randomUUID().toString().replace("-", "");
        createdUsernames.add(username);
        return userRepository.save(new User(username, passwordEncoder.encode("test-password-123")));
    }

    private Cookie login(String username) throws Exception {
        String body = """
                {
                    "username": "%s",
                    "password": "test-password-123"
                }
                """.formatted(username);

        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("sentinel_session");
    }
}
