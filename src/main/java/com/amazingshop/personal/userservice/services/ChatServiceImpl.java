package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.dto.responses.ChatMessagesListResponse;
import com.amazingshop.personal.userservice.dto.responses.ChatsListResponse;
import com.amazingshop.personal.userservice.interfaces.ChatService;
import com.amazingshop.personal.userservice.interfaces.EntityMapper;
import com.amazingshop.personal.userservice.models.Chat;
import com.amazingshop.personal.userservice.models.ChatMessage;
import com.amazingshop.personal.userservice.repositories.ChatMessageRepository;
import com.amazingshop.personal.userservice.repositories.ChatRepository;
import com.amazingshop.personal.userservice.util.exceptions.ChatNotFoundException;
import com.amazingshop.personal.userservice.util.exceptions.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final EntityMapper entityMapper;

    @Autowired
    public ChatServiceImpl(ChatRepository chatRepository, ChatMessageRepository chatMessageRepository,
                           EntityMapper entityMapper) {
        this.chatRepository = chatRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    @Cacheable(value = "userChats", keyGenerator = "userChatsKeyGenerator")
    public ChatsListResponse getUserChats(Long userId, String title, String subject) {
        if (title != null && !title.trim().isEmpty()) {
            return new ChatsListResponse(entityMapper.toChatResponseList(
                    chatRepository.findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(userId, title)
            ));
        }
        if (subject != null && !subject.trim().isEmpty()) {
            return new ChatsListResponse(entityMapper.toChatResponseList(
                    chatRepository.findByUserIdAndSubjectOrderByUpdatedAtDesc(userId, subject)
            ));
        }
        return new ChatsListResponse(entityMapper.toChatResponseList(
                chatRepository.findByUserIdOrderByUpdatedAtDesc(userId)));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userChats", allEntries = true),
            @CacheEvict(value = "recentChats", allEntries = true)
    })
    public Chat createChat(Long userId, String title, String subject) {
        Chat chat = new Chat();
        chat.setUserId(userId);
        chat.setTitle(title != null ? truncateTitle(title) : "New Chat");
        chat.setSubject(subject);
        return chatRepository.save(chat);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userChats", allEntries = true),
            @CacheEvict(value = "recentChats", allEntries = true),
            @CacheEvict(value = "chatMessages", key = "#chatId + ':' + #userId")
    })
    public void deleteChat(Long chatId, Long userId) {
        Chat chat = findChatByIdOrThrow(chatId);
        validateChatOwnership(chat, userId);
        chatRepository.deleteById(chatId);
    }


    @Override
    @Cacheable(value = "chatMessages", key = "#chatId + ':' + #userId")
    public ChatMessagesListResponse getChatMessages(Long chatId, Long userId) {
        Chat chat = findChatByIdOrThrow(chatId);
        validateChatOwnership(chat, userId);

        List<ChatMessage> chatMessageList = chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        return new ChatMessagesListResponse(entityMapper.toChatMessageResponseList(chatMessageList));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userChats", allEntries = true),
            @CacheEvict(value = "recentChats", allEntries = true),
            @CacheEvict(value = "chatMessages", key = "#chatId + ':' + #userId")
    })
    public ChatMessage addMessage(Long chatId, Long userId, String content, String role, String templateUsed) {
        Chat chat = findChatByIdOrThrow(chatId);
        validateChatOwnership(chat, userId);

        // Проверяем, является ли это первым сообщением пользователя
        boolean isFirstUserMessage = chatMessageRepository.countByChatIdAndRole(chatId, "user") == 0;

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

        return chatMessageRepository.save(message);
    }

    // Получить последние N чатов
    @Override
    @Cacheable(value = "recentChats", key = "#userId + ':' + #limit")
    public ChatsListResponse getRecentChats(Long userId, int limit) {
        List<Chat> allChats = chatRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        List<Chat> limitedChats = allChats.stream()
                .limit(limit)
                .toList();
        return new ChatsListResponse(entityMapper.toChatResponseList(limitedChats));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userChats", allEntries = true),
            @CacheEvict(value = "recentChats", allEntries = true)
    })
    public Chat updateChatTitle(Long chatId, Long userId, String newTitle) {
        Chat chat = findChatByIdOrThrow(chatId);
        validateChatOwnership(chat, userId);

        chat.setTitle(newTitle);
        chat.setUpdatedAt(LocalDateTime.now());

        return chatRepository.save(chat);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "userChats", allEntries = true),
            @CacheEvict(value = "recentChats", allEntries = true),
            @CacheEvict(value = "chatMessages", allEntries = true)
    })
    public void deleteAllChats(Long userId) {
        List<Chat> userChats = chatRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        chatRepository.deleteAll(userChats);
    }

    private Chat findChatByIdOrThrow(Long chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Chat not found"));
    }

    private void validateChatOwnership(Chat chat, Long userId) {
        if (!chat.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to access this chat");
        }
    }

    private String truncateTitle(String text) {
        String[] words = text.trim().split("\\s+");
        StringBuilder title = new StringBuilder();

        for (int i = 0; i < Math.min(words.length, 8); i++) {
            if (title.length() + words[i].length() > 40) break;
            if (!title.isEmpty()) title.append(" ");
            title.append(words[i]);
        }

        return !title.isEmpty() ? title.toString() : "New Chat";
    }
}