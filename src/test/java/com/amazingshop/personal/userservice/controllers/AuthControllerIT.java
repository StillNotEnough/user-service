package com.amazingshop.personal.userservice.controllers;

import com.amazingshop.personal.userservice.UserServiceApplication;
import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UserServiceApplication.class)
@AutoConfigureMockMvc
@Transactional
public class AuthControllerIntegrationTest {
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UserDTO testUser;

    @Autowired
    public AuthControllerIntegrationTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void setUp(){
        testUser = new UserDTO("integrationTestUser", "securePass123", "test@integration.com");
    }

    @Test
    void signup_shouldCreateUserAndReturnJwtToken() throws Exception {
        // Act: отправлем POST-запрос на регистрацию
        ResultActions result = mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUser)));

        // Assert: проверяем статус и содержимое ответа
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void signup_shouldReturnBadRequestForInvalidData() throws Exception {
        // Arrange: создаем пользователя с невалидным email
        UserDTO invalidUser = new UserDTO("validUser", "validPass", "invalid-email");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }
}
