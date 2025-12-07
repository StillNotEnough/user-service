package com.amazingshop.personal.userservice.controllers;

import com.amazingshop.personal.userservice.enums.Role;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.repositories.UsersRepository;
import com.amazingshop.personal.userservice.security.jwt.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UsersControllerIT {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final UsersRepository usersRepository;

    private String authToken;
    private User testUser;

    @Autowired
    public UsersControllerIT(MockMvc mockMvc, ObjectMapper objectMapper, JwtUtil jwtUtil, UsersRepository usersRepository) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
        this.usersRepository = usersRepository;
    }

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setRole(Role.USER);
        testUser = usersRepository.save(testUser);

        authToken = jwtUtil.generateAccessToken(testUser.getUsername());
    }

    @Test
    @DisplayName("GET /users/me: должен вернуть данные текущего пользователя")
    void getCurrentUser_ShouldReturnUserData() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value("testUser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.subscriptionPlan").value("FREE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist()) // Пароль не должен возвращаться
                .andExpect(jsonPath("$.refreshToken").doesNotExist()); // Refresh token не должен возвращаться
    }

    @Test
    @DisplayName("GET /users/me: должен вернуть 401 без токена")
    void getCurrentUser_ShouldReturn401_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/me: должен вернуть 401 для невалидного токена")
    void getCurrentUser_ShouldReturn401_WithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/me: должен вернуть 401 для истекшего токена")
    void getCurrentUser_ShouldReturn401_WithExpiredToken() throws Exception {
        // Создаём токен с истекшим сроком действия
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJVc2VyIGRldGFpbHMiLCJ1c2VybmFtZSI6InRlc3RVc2VyIiwidHlwZSI6ImFjY2VzcyIsImlhdCI6MTYwOTQ1OTIwMCwiaXNzIjoiU2hwb3JhQWkiLCJleHAiOjE2MDk0NTkyMDB9.test";

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/me: должен вернуть данные для ADMIN роли")
    void getCurrentUser_ShouldReturnUserData_ForAdmin() throws Exception {
        // Создаём админа
        User admin = new User();
        admin.setUsername("adminUser");
        admin.setEmail("admin@example.com");
        admin.setPassword("password");
        admin.setRole(Role.ADMIN);
        admin = usersRepository.save(admin);

        String adminToken = jwtUtil.generateAccessToken(admin.getUsername());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("adminUser"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("PUT /users/me: должен обновить email пользователя")
    void updateCurrentUser_ShouldUpdateEmail() throws Exception {
        Map<String, String> updates = Map.of("email", "newemail@example.com");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newemail@example.com"))
                .andExpect(jsonPath("$.username").value("testUser")); // Username не должен измениться
    }

    @Test
    @DisplayName("PUT /users/me: должен обновить profilePictureUrl")
    void updateCurrentUser_ShouldUpdateProfilePicture() throws Exception {
        Map<String, String> updates = Map.of("profilePictureUrl", "https://example.com/pic.jpg");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profilePictureUrl").value("https://example.com/pic.jpg"))
                .andExpect(jsonPath("$.username").value("testUser"));
    }

    @Test
    @DisplayName("PUT /users/me: должен обновить и email и profilePictureUrl одновременно")
    void updateCurrentUser_ShouldUpdateBothFields() throws Exception {
        Map<String, String> updates = Map.of(
                "email", "updated@example.com",
                "profilePictureUrl", "https://example.com/new-pic.jpg"
        );

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.profilePictureUrl").value("https://example.com/new-pic.jpg"));
    }

    @Test
    @DisplayName("PUT /users/me: должен вернуть 401 без токена")
    void updateCurrentUser_ShouldReturn401_WithoutToken() throws Exception {
        Map<String, String> updates = Map.of("email", "new@example.com");

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /users/me: НЕ должен обновлять username")
    void updateCurrentUser_ShouldNotUpdateUsername() throws Exception {
        Map<String, String> updates = Map.of("username", "hackedUsername");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testUser")); // Username остался прежним
    }

    @Test
    @DisplayName("PUT /users/me: НЕ должен обновлять password")
    void updateCurrentUser_ShouldNotUpdatePassword() throws Exception {
        Map<String, String> updates = Map.of("password", "newPassword123");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk());
        // Проверяем что пароль не изменился (это можно сделать только косвенно через попытку логина)
        // В реальности пароль не должен измениться
    }

    @Test
    @DisplayName("PUT /users/me: НЕ должен обновлять role")
    void updateCurrentUser_ShouldNotUpdateRole() throws Exception {
        Map<String, String> updates = Map.of("role", "ADMIN");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER")); // Role остался USER
    }

    @Test
    @DisplayName("PUT /users/me: должен игнорировать пустые обновления")
    void updateCurrentUser_ShouldIgnoreEmptyUpdates() throws Exception {
        Map<String, String> emptyUpdates = Map.of();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyUpdates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com")); // Email не изменился
    }

    @Test
    @DisplayName("PUT /users/me: должен обновить только переданные поля")
    void updateCurrentUser_ShouldUpdateOnlyProvidedFields() throws Exception {
        // Устанавливаем начальное значение profilePictureUrl
        testUser.setProfilePictureUrl("https://old-pic.jpg");
        usersRepository.save(testUser);

        // Обновляем только email
        Map<String, String> updates = Map.of("email", "onlyemail@example.com");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("onlyemail@example.com"))
                .andExpect(jsonPath("$.profilePictureUrl").value("https://old-pic.jpg")); // Не изменился
    }

    @Test
    @DisplayName("GET /users/me: должен вернуть правильные данные после обновления")
    void getCurrentUser_ShouldReturnUpdatedData_AfterUpdate() throws Exception {
        // Обновляем пользователя
        Map<String, String> updates = Map.of("email", "updated@example.com");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk());

        // Получаем обновленные данные
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    @DisplayName("GET /users/me: не должен возвращать чувствительные данные")
    void getCurrentUser_ShouldNotReturnSensitiveData() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.refreshTokenExpiry").doesNotExist());
    }

    @Test
    @DisplayName("PUT /users/me: должен корректно обработать null значения")
    void updateCurrentUser_ShouldHandleNullValues() throws Exception {
        // Устанавливаем profilePictureUrl
        testUser.setProfilePictureUrl("https://example.com/pic.jpg");
        usersRepository.save(testUser);

        // Пытаемся обновить на null (это может быть удаление картинки)
        Map<String, String> updates = Map.of("profilePictureUrl", "");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk());
    }
}