package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.interfaces.ChatService;
import com.amazingshop.personal.userservice.models.Chat;
import com.amazingshop.personal.userservice.models.ChatMessage;
import com.amazingshop.personal.userservice.repositories.ChatRepository;
import com.amazingshop.personal.userservice.repositories.ChatMessageRepository;
import com.amazingshop.personal.userservice.util.exceptions.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Autowired
    public ChatServiceImpl(ChatRepository chatRepository, ChatMessageRepository messageRepository, ChatMessageRepository chatMessageRepository) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Override
    public List<Chat> getUserChats(Long userId, String search, String subject) {
        if (search != null && !search.trim().isEmpty()) {
            return chatRepository.findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(userId, search);
        }
        if (subject != null && !subject.trim().isEmpty()) {
            return chatRepository.findByUserIdAndSubjectOrderByUpdatedAtDesc(userId, subject);
        }
        return chatRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Override
    @Transactional
    public Chat createChat(Long userId, String title, String subject) {
        Chat chat = new Chat();
        chat.setUserId(userId);
        chat.setTitle(title != null ? truncateTitle(title) : "New Chat");
        chat.setSubject(subject);
        return chatRepository.save(chat);
    }

    @Override
    @Transactional
    public void deleteChat(Long chatId, Long userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chat.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this chat");
        }

        chatRepository.deleteById(chatId);
    }


    @Override
    public List<ChatMessage> getChatMessages(Long chatId, Long userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chat.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this chat");
        }

        return messageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
    }

    @Override
    @Transactional
    public ChatMessage addMessage(Long chatId, Long userId, String content, String role, String templateUsed) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chat.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this chat");
        }

        // Проверяем, является ли это первым сообщением пользователя
        boolean isFirstUserMessage = messageRepository.countByChatIdAndRole(chatId, "user") == 0;

        ChatMessage message = new ChatMessage();
        message.setChatId(chatId);
        message.setContent(content);
        message.setRole(role);
        message.setTemplateUsed(templateUsed);
        message.setCreatedAt(LocalDateTime.now());

        chatMessageRepository.save(message);

        // Если это первое сообщение пользователя, обновляем title чата
        if (isFirstUserMessage && "user".equals(role) && content != null && !content.trim().isEmpty()) {
            String newTitle = truncateTitle(content);
            chat.setTitle(newTitle);
            log.info("Auto-generated chat title from first message: {}", newTitle);
        }

        // Update chat timestamp
        chat.setUpdatedAt(LocalDateTime.now());
        chatRepository.save(chat);

        return messageRepository.save(message);
    }

    private String truncateTitle(String text) {
        String[] words = text.trim().split("\\s+");
        StringBuilder title = new StringBuilder();

        for (int i = 0; i < Math.min(words.length, 8); i++) {
            if (title.length() + words[i].length() > 40) break;
            if (title.length() > 0) title.append(" ");
            title.append(words[i]);
        }

        return title.length() > 0 ? title.toString() : "New Chat";
    }

    @Override
    // Получить последние N чатов
    public List<Chat> getRecentChats(Long userId, int limit) {
        List<Chat> allChats = chatRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        return allChats.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Chat updateChatTitle(Long chatId, Long userId, String newTitle) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chat.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this chat");
        }

        chat.setTitle(newTitle);
        chat.setUpdatedAt(LocalDateTime.now());

        return chatRepository.save(chat);
    }

    @Override
    public void deleteAllChats(Long userId) {
        List<Chat> userChats = chatRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        chatRepository.deleteAll(userChats);
    }
}