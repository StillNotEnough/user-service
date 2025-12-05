package com.amazingshop.personal.userservice.controllers;

import com.amazingshop.personal.userservice.repositories.UsersRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",

        "jwt.secret=super-secret-test-key-for-hs256-12345678901234567890",
        "jwt.access-token-expiration=900000",
        "jwt.refresh-token-expiration=604800000"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AuthControllerIT{

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final MockMvc mockMvc;

    @Autowired
    public AuthControllerIT(UsersRepository usersRepository, PasswordEncoder passwordEncoder, MockMvc mockMvc) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp(){
        usersRepository.deleteAll();
    }

    private void registerUser(String username, String email, String password) throws Exception {
        String body = """
            {
                "username": "%s",
                "email": "%s",
                "password": "%s"
            }
            """.formatted(username, email, password);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    private String getRefreshToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken");
    }

    @Test
    void signup_SuccessfulRegistration_ShouldReturnTokens() throws Exception {
        registerUser("newUser", "new@example.com", "ValidPass123!");
    }

    @Test
    void signup_ValidationError_ShouldReturn400() throws Exception {
        String invalidRequest = """
            {
                "username": "",
                "email": "not-an-email",
                "password": "123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_SuccessfulLogin_ShouldReturnTokenPair() throws Exception {
        registerUser("john", "john@example.com", "pass123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"john\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_InvalidCredentials_ShouldReturn401() throws Exception {
        registerUser("alice", "alice@example.com", "correctPass");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrongPass\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_ValidRefreshToken_ShouldReturnNewTokenPair() throws Exception {
        registerUser("bob", "bob@example.com", "pass123");
        String refreshToken = getRefreshToken("bob", "pass123");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_InvalidRefreshToken_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"definitely.invalid.token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_ValidRefreshToken_ShouldInvalidateIt() throws Exception {
        registerUser("dave", "dave@example.com", "pass123");
        String refreshToken = getRefreshToken("dave", "pass123");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk());

        // Повторный refresh — должен быть 401
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}