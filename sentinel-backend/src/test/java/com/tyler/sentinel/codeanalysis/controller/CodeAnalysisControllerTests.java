package com.tyler.sentinel.codeanalysis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tyler.sentinel.codeanalysis.repository.CodeRelationshipRepository;
import com.tyler.sentinel.codeanalysis.repository.CodeSymbolRepository;
import com.tyler.sentinel.model.User;
import com.tyler.sentinel.repository.UserRepository;
import com.tyler.sentinel.repository.UserSessionRepository;
import com.tyler.sentinel.repositoryimport.entity.Project;
import com.tyler.sentinel.repositoryimport.entity.ProjectFile;
import com.tyler.sentinel.repositoryimport.repository.ProjectFileRepository;
import com.tyler.sentinel.repositoryimport.repository.ProjectRepository;
import jakarta.servlet.http.Cookie;
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
class CodeAnalysisControllerTests {

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

    @Autowired
    private CodeRelationshipRepository codeRelationshipRepository;

    @Autowired
    private CodeSymbolRepository codeSymbolRepository;

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
    void parseProjectCreatesStructuralAndDependencyRelationships() throws Exception {
        User user = createUser("analysis");
        Cookie cookie = login(user.getUsername());
        Project project = projectRepository.save(new Project(
                user,
                "Analysis Project",
                "Code graph test",
                "https://github.com/example/analysis-project",
                "analysis-project",
                "example",
                "IMPORTED"
        ));
        projectFileRepository.save(new ProjectFile(
                project,
                "src/main/java/example/UserRepository.java",
                "UserRepository.java",
                "java",
                "Java",
                "SOURCE",
                160L,
                "1".repeat(64),
                """
                        package example;

                        public interface UserRepository {
                            void save(User user);
                        }
                        """
        ));
        projectFileRepository.save(new ProjectFile(
                project,
                "src/main/java/example/UserService.java",
                "UserService.java",
                "java",
                "Java",
                "SOURCE",
                320L,
                "2".repeat(64),
                """
                        package example;

                        public class UserService extends BaseService implements UserHandler {
                            private UserRepository repository;

                            public User create(User user) {
                                repository.save(user);
                                return user;
                            }
                        }
                        """
        ));

        mockMvc.perform(post("/api/projects/" + project.getId() + "/code/parse")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsedFiles").value(2));

        String relationshipsJson = mockMvc.perform(get("/api/projects/" + project.getId() + "/code/relationships")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(codeSymbolRepository.countByProjectId(project.getId())).isGreaterThan(0);
        assertThat(codeRelationshipRepository.countByProjectId(project.getId())).isGreaterThan(0);
        assertThat(relationshipsJson)
                .contains("\"relationshipType\":\"CALLS\"")
                .contains("\"sourceName\":\"create\"")
                .contains("\"targetName\":\"save\"")
                .contains("\"relationshipType\":\"USES_FIELD_TYPE\"")
                .contains("\"sourceName\":\"repository\"")
                .contains("\"targetName\":\"UserRepository\"");

        String searchJson = mockMvc.perform(get("/api/code-search")
                        .param("query", "save")
                        .param("projectId", project.getId().toString())
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCount").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(searchJson)
                .contains("\"resultType\":\"RELATIONSHIP\"")
                .contains("\"relationshipType\":\"CALLS\"")
                .contains("\"targetName\":\"save\"");
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
