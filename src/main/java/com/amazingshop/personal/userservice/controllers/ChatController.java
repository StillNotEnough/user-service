package com.amazingshop.personal.userservice.controllers;

import com.amazingshop.personal.userservice.dto.requests.AddMessageRequest;
import com.amazingshop.personal.userservice.dto.requests.CreateChatRequest;
import com.amazingshop.personal.userservice.dto.requests.UpdateChatTitleRequest;
import com.amazingshop.personal.userservice.dto.responses.ChatMessageResponse;
import com.amazingshop.personal.userservice.dto.responses.ChatMessagesListResponse;
import com.amazingshop.personal.userservice.dto.responses.ChatResponse;
import com.amazingshop.personal.userservice.dto.responses.ChatsListResponse;
import com.amazingshop.personal.userservice.interfaces.ChatService;
import com.amazingshop.personal.userservice.interfaces.EntityMapper;
import com.amazingshop.personal.userservice.interfaces.UserService;
import com.amazingshop.personal.userservice.models.Chat;
import com.amazingshop.personal.userservice.models.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/chats")
public class ChatController {
    private final ChatService chatService;
    private final UserService userService;
    private final EntityMapper entityMapper;

    @Autowired
    public ChatController(ChatService chatService, UserService userService, EntityMapper entityMapper) {
        this.chatService = chatService;
        this.userService = userService;
        this.entityMapper = entityMapper;
    }

    @GetMapping
    public ResponseEntity<ChatsListResponse> getChats(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String subject
    ) {
        ChatsListResponse response = chatService.getUserChats(userService.getCurrentUserId(), search, subject);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ChatResponse> createChat(@RequestBody CreateChatRequest request) {
        Chat chat = chatService.createChat(userService.getCurrentUserId(), request.getTitle(), request.getSubject());
        return ResponseEntity.ok(entityMapper.toChatResponse(chat));
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long chatId) {
        chatService.deleteChat(chatId, userService.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<ChatMessagesListResponse> getChatMessages(@PathVariable Long chatId) {
        ChatMessagesListResponse response = chatService.getChatMessages(chatId, userService.getCurrentUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ChatMessageResponse> addMessage(@PathVariable Long chatId,
                                                          @RequestBody AddMessageRequest request
    ) {
        ChatMessage message = chatService.addMessage(
                chatId, userService.getCurrentUserId(), request.getContent(), request.getRole(), request.getTemplateUsed()
        );
        return ResponseEntity.ok(entityMapper.toChatMessageResponse(message));
    }

    // Получить последние чаты для sidebar
    @GetMapping("/recent")
    public ResponseEntity<ChatsListResponse> getRecentChats(
            @RequestParam(defaultValue = "20") int limit) {
        ChatsListResponse response = chatService.getRecentChats(userService.getCurrentUserId(), limit);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{chatId}/title")
    public ResponseEntity<ChatResponse> updateChatTitle(
            @PathVariable Long chatId,
            @RequestBody UpdateChatTitleRequest request) {
        Chat updatedChat = chatService.updateChatTitle(chatId, userService.getCurrentUserId(), request.getNewTitle());
        return ResponseEntity.ok(entityMapper.toChatResponse(updatedChat));
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllChats() {
        chatService.deleteAllChats(userService.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}