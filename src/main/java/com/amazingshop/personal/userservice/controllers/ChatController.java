package com.amazingshop.personal.userservice.controllers;

import com.amazingshop.personal.userservice.dto.requests.AddMessageRequest;
import com.amazingshop.personal.userservice.dto.requests.CreateChatRequest;
import com.amazingshop.personal.userservice.dto.requests.UpdateChatTitleRequest;
import com.amazingshop.personal.userservice.interfaces.ChatService;
import com.amazingshop.personal.userservice.interfaces.UserService;
import com.amazingshop.personal.userservice.models.Chat;
import com.amazingshop.personal.userservice.models.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/chats")
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @Autowired
    public ChatController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }


    @GetMapping
    public ResponseEntity<List<Chat>> getChats(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String subject
    ) {
        List<Chat> chats = chatService.getUserChats(userService.getCurrentUserId(), search, subject);
        return ResponseEntity.ok(chats);
    }

    @PostMapping
    public ResponseEntity<Chat> createChat(@RequestBody CreateChatRequest request) {
        Chat chat = chatService.createChat(userService.getCurrentUserId(), request.getTitle(), request.getSubject());
        return ResponseEntity.ok(chat);
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long chatId) {
        chatService.deleteChat(chatId, userService.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatMessage>> getChatMessages(@PathVariable Long chatId) {
        List<ChatMessage> messages = chatService.getChatMessages(chatId, userService.getCurrentUserId());
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ChatMessage> addMessage(@PathVariable Long chatId,
                                                  @RequestBody AddMessageRequest request
    ) {
        ChatMessage message = chatService.addMessage(
                chatId, userService.getCurrentUserId(), request.getContent(), request.getRole(), request.getTemplateUsed()
        );
        return ResponseEntity.ok(message);
    }

    // Получить последние чаты для sidebar
    @GetMapping("/recent")
    public ResponseEntity<List<Chat>> getRecentChats(
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<Chat> chats = chatService.getRecentChats(userService.getCurrentUserId(), limit);
        return ResponseEntity.ok(chats);
    }

    @PutMapping("/{chatId}/title")
    public ResponseEntity<Chat> updateChatTitle(
            @PathVariable Long chatId,
            @RequestBody UpdateChatTitleRequest request) {
        Chat updatedChat = chatService.updateChatTitle(chatId, userService.getCurrentUserId(), request.getNewTitle());
        return ResponseEntity.ok(updatedChat);
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllChats() {
        chatService.deleteAllChats(userService.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}