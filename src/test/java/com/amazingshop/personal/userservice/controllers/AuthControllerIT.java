package com.amazingshop.personal.userservice.controllers;

import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.amazingshop.personal.userservice.repositories.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional // Автоматический откат транзакций после каждого теста
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    AuthControllerIT(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    @DisplayName("POST /auth/signup: должен зарегистрировать пользователя и вернуть токены")
    void signup_ShouldRegisterUser_AndReturnTokens() throws Exception {
        // Arrange
        UserDTO userDTO = new UserDTO("newUser", "SecurePass123!", "new@example.com");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.username").value("newUser"))
                .andExpect(jsonPath("$.accessTokenExpiresIn").isNumber())
                .andExpect(jsonPath("$.refreshTokenExpiresIn").isNumber());
    }

    @Test
    @DisplayName("POST /auth/signup: должен вернуть 400 для невалидных данных")
    void signup_ShouldReturn400_WhenValidationFails() throws Exception {
        // Arrange - короткий username и невалидный email
        UserDTO invalidUser = new UserDTO("a", "pass123", "not-an-email");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @DisplayName("POST /auth/signup: должен вернуть 400 при дублировании username")
    void signup_ShouldReturn400_WhenUsernameDuplicate() throws Exception {
        // Arrange - регистрируем первого пользователя
        registerUser("existingUser", "existing@example.com", "Pass123!");

        // Пытаемся зарегистрировать второго с тем же username
        UserDTO duplicateUser = new UserDTO("existingUser", "Pass123!", "another@example.com");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/signup: должен вернуть 400 при дублировании email")
    void signup_ShouldReturn400_WhenEmailDuplicate() throws Exception {
        // Arrange
        registerUser("user1", "duplicate@example.com", "Pass123!");

        UserDTO duplicateEmail = new UserDTO("user2", "Pass123!", "duplicate@example.com");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmail)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login: должен вернуть токены для валидных credentials")
    void login_ShouldReturnTokens_WhenCredentialsValid() throws Exception {
        // Arrange - регистрируем пользователя
        registerUser("john", "john@example.com", "Pass123!");

        String loginRequest = """
            {
                "username": "john",
                "password": "Pass123!"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    @DisplayName("POST /auth/login: должен вернуть 401 для неверного пароля")
    void login_ShouldReturn401_WhenPasswordIncorrect() throws Exception {
        // Arrange
        registerUser("alice", "alice@example.com", "CorrectPass123!");

        String loginRequest = """
            {
                "username": "alice",
                "password": "WrongPass123!"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login: должен вернуть 401 для несуществующего пользователя")
    void login_ShouldReturn401_WhenUserNotExists() throws Exception {
        String loginRequest = """
            {
                "username": "nonExistentUser",
                "password": "SomePass123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/refresh: должен вернуть новые токены для валидного refresh token")
    void refresh_ShouldReturnNewTokens_WhenRefreshTokenValid() throws Exception {
        // Arrange - регистрируем и получаем refresh token
        registerUser("bob", "bob@example.com", "Pass123!");
        String refreshToken = loginAndGetRefreshToken("bob", "Pass123!");

        String refreshRequest = """
            {
                "refreshToken": "%s"
            }
            """.formatted(refreshToken);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    @DisplayName("POST /auth/refresh: должен вернуть 401 для невалидного refresh token")
    void refresh_ShouldReturn401_WhenRefreshTokenInvalid() throws Exception {
        String invalidRefreshRequest = """
            {
                "refreshToken": "invalid.jwt.token"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRefreshRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/refresh: должен вернуть 401 после logout")
    void refresh_ShouldReturn401_AfterLogout() throws Exception {
        // Arrange
        registerUser("charlie", "charlie@example.com", "Pass123!");
        String refreshToken = loginAndGetRefreshToken("charlie", "Pass123!");

        // Logout
        logout(refreshToken);

        // Act & Assert - refresh должен вернуть 401
        String refreshRequest = """
            {
                "refreshToken": "%s"
            }
            """.formatted(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/logout: должен инвалидировать refresh token")
    void logout_ShouldInvalidateRefreshToken() throws Exception {
        // Arrange
        registerUser("dave", "dave@example.com", "Pass123!");
        String refreshToken = loginAndGetRefreshToken("dave", "Pass123!");

        String logoutRequest = """
            {
                "refreshToken": "%s"
            }
            """.formatted(refreshToken);

        // Act
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutRequest))
                .andExpect(status().isOk());

        // Assert - проверяем что refresh token больше не работает
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/logout: должен вернуть 401 для невалидного refresh token")
    void logout_ShouldReturn401_WhenRefreshTokenInvalid() throws Exception {
        String invalidLogoutRequest = """
            {
                "refreshToken": "invalid.token"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidLogoutRequest))
                .andExpect(status().isUnauthorized());
    }

    private void registerUser(String username, String email, String password) throws Exception {
        UserDTO userDTO = new UserDTO(username, password, email);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetRefreshToken(String username, String password) throws Exception {
        String loginRequest = """
            {
                "username": "%s",
                "password": "%s"
            }
            """.formatted(username, password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken");
    }

    private void logout(String refreshToken) throws Exception {
        String logoutRequest = """
            {
                "refreshToken": "%s"
            }
            """.formatted(refreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutRequest))
                .andExpect(status().isOk());
    }
}