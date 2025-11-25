package com.amazingshop.personal.userservice.interfaces;

import com.amazingshop.personal.userservice.models.Chat;
import com.amazingshop.personal.userservice.models.ChatMessage;

import java.util.List;

public interface ChatService {
    List<Chat> getUserChats(Long userId, String search, String subject);
    Chat createChat(Long userId, String title, String subject);
    void deleteChat(Long chatId, Long userId);
    List<ChatMessage> getChatMessages(Long chatId, Long useId);
    ChatMessage addMessage(Long chatId, Long userId, String content, String role, String templateUsed);
    List<Chat> getRecentChats(Long userId, int limit);
    Chat updateChatTitle(Long chatId, Long userId, String newTitle);
    void deleteAllChats(Long userId);
}

