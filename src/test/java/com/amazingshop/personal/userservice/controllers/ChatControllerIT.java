package com.amazingshop.personal.userservice.controllers;

import com.amazingshop.personal.userservice.dto.requests.AddMessageRequest;
import com.amazingshop.personal.userservice.dto.requests.CreateChatRequest;
import com.amazingshop.personal.userservice.dto.requests.UpdateChatTitleRequest;
import com.amazingshop.personal.userservice.models.Chat;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.repositories.ChatRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerIT {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final UsersRepository usersRepository;
    private final ChatRepository chatRepository;

    private String authToken;
    private User testUser;

    @Autowired
    ChatControllerIT(MockMvc mockMvc, ObjectMapper objectMapper, JwtUtil jwtUtil, UsersRepository usersRepository, ChatRepository chatRepository) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
        this.usersRepository = usersRepository;
        this.chatRepository = chatRepository;
    }

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("chatTestUser");
        testUser.setEmail("chat@test.com");
        testUser.setPassword("password");
        testUser = usersRepository.save(testUser);

        authToken = jwtUtil.generateAccessToken(testUser.getUsername());
    }

    @Test
    @DisplayName("POST /chats: должен создать новый чат")
    void createChat_ShouldReturnCreatedChat() throws Exception {
        CreateChatRequest request = new CreateChatRequest("Test Chat", "MATH");

        mockMvc.perform(post("/api/v1/chats")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Chat"))
                .andExpect(jsonPath("$.subject").value("MATH"))
                .andExpect(jsonPath("$.userId").value(testUser.getId()));
    }

    @Test
    @DisplayName("POST /chats: должен вернуть 401 без токена")
    void createChat_ShouldReturn401_WithoutToken() throws Exception {
        CreateChatRequest request = new CreateChatRequest("Test", "MATH");

        mockMvc.perform(post("/api/v1/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /chats: должен вернуть список чатов пользователя")
    void getChats_ShouldReturnUserChats() throws Exception {
        createTestChat("Chat 1", "MATH");
        createTestChat("Chat 2", "PROGRAMMING");

        mockMvc.perform(get("/api/v1/chats")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Chat 2")))
                .andExpect(jsonPath("$[1].title", is("Chat 1")));
    }

    @Test
    @DisplayName("GET /chats?search=: должен фильтровать чаты по поиску")
    void getChats_ShouldFilterBySearch() throws Exception {
        createTestChat("Math homework", "MATH");
        createTestChat("Programming task", "PROGRAMMING");

        mockMvc.perform(get("/api/v1/chats")
                        .param("search", "math")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", containsString("Math")));
    }

    // ==================== DELETE CHAT ====================

    @Test
    @DisplayName("DELETE /chats/{id}: должен удалить чат")
    void deleteChat_ShouldDeleteChat() throws Exception {
        Chat chat = createTestChat("To be deleted", "MATH");

        mockMvc.perform(delete("/api/v1/chats/" + chat.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/chats")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ✅ ДОБАВЛЕН НОВЫЙ ТЕСТ
    @Test
    @DisplayName("DELETE /chats/{id}: должен вернуть 403 при попытке удалить чужой чат")
    void deleteChat_ShouldReturn403_WhenNotOwner() throws Exception {
        // Создаём второго пользователя
        User otherUser = new User();
        otherUser.setUsername("otherUser");
        otherUser.setEmail("other@test.com");
        otherUser.setPassword("password");
        otherUser = usersRepository.save(otherUser);
        // Создаём чат от имени другого пользователя
        Chat otherUserChat = new Chat();
        otherUserChat.setUserId(otherUser.getId());
        otherUserChat.setTitle("Other's chat");
        otherUserChat = chatRepository.save(otherUserChat);

        // Пытаемся удалить чужой чат
        mockMvc.perform(delete("/api/v1/chats/" + otherUserChat.getId())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("not authorized")));
    }

// ==================== MESSAGES ====================

    @Test
    @DisplayName("POST /chats/{id}/messages: должен добавить сообщение")
    void addMessage_ShouldAddMessageToChat() throws Exception {
        Chat chat = createTestChat("Test Chat", null);
        AddMessageRequest request = new AddMessageRequest("Hello!", "user", null);

        mockMvc.perform(post("/api/v1/chats/" + chat.getId() + "/messages")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello!"))
                .andExpect(jsonPath("$.role").value("user"));
    }

    @Test
    @DisplayName("GET /chats/{id}/messages: должен вернуть сообщения чата")
    void getChatMessages_ShouldReturnMessages() throws Exception {
        Chat chat = createTestChat("Test", null);

        AddMessageRequest msg1 = new AddMessageRequest("First", "user", null);
        AddMessageRequest msg2 = new AddMessageRequest("Second", "assistant", null);

        mockMvc.perform(post("/api/v1/chats/" + chat.getId() + "/messages")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(msg1)));

        mockMvc.perform(post("/api/v1/chats/" + chat.getId() + "/messages")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(msg2)));

        mockMvc.perform(get("/api/v1/chats/" + chat.getId() + "/messages")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content").value("First"))
                .andExpect(jsonPath("$[1].content").value("Second"));
    }

    // ✅ ДОБАВЛЕН НОВЫЙ ТЕСТ
    @Test
    @DisplayName("GET /chats/{id}/messages: должен вернуть 403 для чужого чата")
    void getChatMessages_ShouldReturn403_WhenNotOwner() throws Exception {
        // Создаём другого пользователя и его чат
        User otherUser = new User();
        otherUser.setUsername("otherUser2");
        otherUser.setEmail("other2@test.com");
        otherUser.setPassword("password");
        otherUser = usersRepository.save(otherUser);

        Chat otherUserChat = new Chat();
        otherUserChat.setUserId(otherUser.getId());
        otherUserChat.setTitle("Other's chat");
        otherUserChat = chatRepository.save(otherUserChat);

        // Пытаемся получить сообщения чужого чата
        mockMvc.perform(get("/api/v1/chats/" + otherUserChat.getId() + "/messages")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }

// ==================== UPDATE TITLE ====================

    @Test
    @DisplayName("PUT /chats/{id}/title: должен обновить title чата")
    void updateChatTitle_ShouldUpdateTitle() throws Exception {
        Chat chat = createTestChat("Old Title", null);
        UpdateChatTitleRequest request = new UpdateChatTitleRequest("New Title");

        mockMvc.perform(put("/api/v1/chats/" + chat.getId() + "/title")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    // ✅ ДОБАВЛЕН НОВЫЙ ТЕСТ
    @Test
    @DisplayName("PUT /chats/{id}/title: должен вернуть 403 для чужого чата")
    void updateChatTitle_ShouldReturn403_WhenNotOwner() throws Exception {
        User otherUser = new User();
        otherUser.setUsername("otherUser3");
        otherUser.setEmail("other3@test.com");
        otherUser.setPassword("password");
        otherUser = usersRepository.save(otherUser);

        Chat otherUserChat = new Chat();
        otherUserChat.setUserId(otherUser.getId());
        otherUserChat.setTitle("Other's chat");
        otherUserChat = chatRepository.save(otherUserChat);

        UpdateChatTitleRequest request = new UpdateChatTitleRequest("Hacked Title");

        mockMvc.perform(put("/api/v1/chats/" + otherUserChat.getId() + "/title")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

// ==================== DELETE ALL ====================

    @Test
    @DisplayName("DELETE /chats/all: должен удалить все чаты пользователя")
    void deleteAllChats_ShouldDeleteAllChats() throws Exception {
        createTestChat("Chat 1", null);
        createTestChat("Chat 2", null);
        createTestChat("Chat 3", null);

        mockMvc.perform(delete("/api/v1/chats/all")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/chats")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(jsonPath("$", hasSize(0)));
    }

// ==================== HELPER ====================

    private Chat createTestChat(String title, String subject) {
        Chat chat = new Chat();
        chat.setUserId(testUser.getId());
        chat.setTitle(title);
        chat.setSubject(subject);
        return chatRepository.save(chat);
    }
}