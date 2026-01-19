package com.amazingshop.personal.userservice.interfaces;

import com.amazingshop.personal.userservice.dto.responses.ChatMessagesListResponse;
import com.amazingshop.personal.userservice.dto.responses.ChatsListResponse;
import com.amazingshop.personal.userservice.models.Chat;
import com.amazingshop.personal.userservice.models.ChatMessage;

public interface ChatService {
    ChatsListResponse getUserChats(Long userId, String search, String subject);
    Chat createChat(Long userId, String title, String subject);
    void deleteChat(Long chatId, Long userId);
    ChatMessagesListResponse getChatMessages(Long chatId, Long useId); // todo
    ChatMessage addMessage(Long chatId, Long userId, String content, String role, String templateUsed);
    ChatsListResponse getRecentChats(Long userId, int limit);
    Chat updateChatTitle(Long chatId, Long userId, String newTitle);
    void deleteAllChats(Long userId);
}

