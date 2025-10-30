package com.amazingshop.personal.userservice.controllers;

import com.amazingshop.personal.userservice.dto.requests.AddMessageRequest;
import com.amazingshop.personal.userservice.dto.requests.CreateChatRequest;
import com.amazingshop.personal.userservice.models.Chat;
import com.amazingshop.personal.userservice.models.ChatMessage;
import com.amazingshop.personal.userservice.security.details.UserDetailsImpl;
import com.amazingshop.personal.userservice.services.ChatService;
import com.amazingshop.personal.userservice.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chats")
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @Autowired
    public ChatController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        return userService.findPersonByPersonName(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    @GetMapping
    public ResponseEntity<List<Chat>> getChats(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String subject
    ) {
        Long userId = getCurrentUserId();
        List<Chat> chats = chatService.getUserChats(userId, search, subject);
        return ResponseEntity.ok(chats);
    }

    @PostMapping
    public ResponseEntity<Chat> createChat(@RequestBody CreateChatRequest request) {
        Long userId = getCurrentUserId();
        Chat chat = chatService.createChat(userId, request.getTitle(), request.getSubject());
        return ResponseEntity.ok(chat);
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long chatId) {
        Long userId = getCurrentUserId();
        chatService.deleteChat(chatId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatMessage>> getChatMessages(@PathVariable Long chatId) {
        Long userId = getCurrentUserId();
        List<ChatMessage> messages = chatService.getChatMessages(chatId, userId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ChatMessage> addMessage(
            @PathVariable Long chatId,
            @RequestBody AddMessageRequest request
    ) {
        Long userId = getCurrentUserId();
        ChatMessage message = chatService.addMessage(
                chatId, userId, request.getContent(), request.getRole(), request.getTemplateUsed()
        );
        return ResponseEntity.ok(message);
    }

    // Получить последние чаты для sidebar
    @GetMapping("/recent")
    public ResponseEntity<List<Chat>> getRecentChats(
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long userId = getCurrentUserId();
        List<Chat> chats = chatService.getRecentChats(userId, limit);
        return ResponseEntity.ok(chats);
    }
}