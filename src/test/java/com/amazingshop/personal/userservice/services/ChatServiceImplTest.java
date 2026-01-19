package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.dto.responses.ChatMessageResponse;
import com.amazingshop.personal.userservice.dto.responses.ChatMessagesListResponse;
import com.amazingshop.personal.userservice.dto.responses.ChatResponse;
import com.amazingshop.personal.userservice.dto.responses.ChatsListResponse;
import com.amazingshop.personal.userservice.interfaces.EntityMapper;
import com.amazingshop.personal.userservice.models.Chat;
import com.amazingshop.personal.userservice.models.ChatMessage;
import com.amazingshop.personal.userservice.repositories.ChatMessageRepository;
import com.amazingshop.personal.userservice.repositories.ChatRepository;
import com.amazingshop.personal.userservice.util.exceptions.ChatNotFoundException;
import com.amazingshop.personal.userservice.util.exceptions.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceImplTest {

    @Mock
    private EntityMapper entityMapper;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    private static final Long USER_ID = 1L;
    private static final Long CHAT_ID = 10L;
    private static final Long OTHER_USER_ID = 999L;

    @Test
    @DisplayName("createChat: должен создать чат с заданным title")
    void createChat_ShouldCreateChatWithTitle() {
        // Arrange
        String title = "Test Chat";
        String subject = "MATH";

        Chat savedChat = new Chat();
        savedChat.setId(CHAT_ID);
        savedChat.setUserId(USER_ID);
        savedChat.setTitle(title);
        savedChat.setSubject(subject);

        when(chatRepository.save(any(Chat.class))).thenReturn(savedChat);

        // Act
        Chat result = chatService.createChat(USER_ID, title, subject);

        // Assert
        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        assertEquals(title, result.getTitle());
        assertEquals(subject, result.getSubject());

        verify(chatRepository, times(1)).save(any(Chat.class));
    }

    @Test
    @DisplayName("createChat: должен создать чат с дефолтным title если title = null")
    void createChat_ShouldUseDefaultTitle_WhenTitleIsNull() {
        // Arrange
        when(chatRepository.save(any(Chat.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Chat result = chatService.createChat(USER_ID, null, "MATH");

        // Assert
        assertEquals("New Chat", result.getTitle());
    }

    @Test
    @DisplayName("createChat: должен обрезать слишком длинный title")
    void createChat_ShouldTruncateLongTitle() {
        // Arrange
        String longTitle = "This is a very long title that exceeds the maximum allowed length of 40 characters";
        when(chatRepository.save(any(Chat.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Chat result = chatService.createChat(USER_ID, longTitle, null);

        // Assert
        assertTrue(result.getTitle().length() <= 40, "Title должен быть <= 40 символов");
    }

    @Test
    @DisplayName("getUserChats: должен вернуть все чаты пользователя")
    void getUserChats_ShouldReturnAllChats() {
        // Arrange
        List<Chat> mockChats = List.of(
                createMockChat(1L, "Chat 1"),
                createMockChat(2L, "Chat 2")
        );
        when(chatRepository.findByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(mockChats);

        // Mock маппера
        List<ChatResponse> mockResponses = List.of(
                ChatResponse.builder().id(1L).title("Chat 1").build(),
                ChatResponse.builder().id(2L).title("Chat 2").build()
        );
        when(entityMapper.toChatResponseList(mockChats)).thenReturn(mockResponses);

        // Act
        ChatsListResponse result = chatService.getUserChats(USER_ID, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getChats().size());
        assertEquals("Chat 1", result.getChats().get(0).getTitle());
        assertEquals("Chat 2", result.getChats().get(1).getTitle());

        verify(chatRepository, times(1)).findByUserIdOrderByUpdatedAtDesc(USER_ID);
        verify(entityMapper, times(1)).toChatResponseList(mockChats);
    }

    @Test
    @DisplayName("getUserChats: должен фильтровать по search")
    void getUserChats_ShouldFilterBySearch() {
        // Arrange
        String search = "test";
        List<Chat> mockChats = List.of(createMockChat(1L, "Test Chat"));
        List<ChatResponse> mockResponses = List.of(
                ChatResponse.builder().id(1L).title("Test Chat").build()
        );

        when(chatRepository.findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(USER_ID, search))
                .thenReturn(mockChats);
        when(entityMapper.toChatResponseList(mockChats)).thenReturn(mockResponses);

        // Act
        ChatsListResponse result = chatService.getUserChats(USER_ID, search, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getChats().size());
        assertEquals("Test Chat", result.getChats().get(0).getTitle());

        verify(chatRepository, times(1))
                .findByUserIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(USER_ID, search);
        verify(entityMapper, times(1)).toChatResponseList(mockChats);
    }

    @Test
    @DisplayName("getUserChats: должен фильтровать по subject")
    void getUserChats_ShouldFilterBySubject() {
        // Arrange
        String subject = "MATH";
        List<Chat> mockChats = List.of(createMockChat(1L, "Math Chat"));
        List<ChatResponse> mockResponses = List.of(
                ChatResponse.builder()
                        .id(1L)
                        .title("Math Chat")
                        .subject(subject)
                        .build()
        );

        when(chatRepository.findByUserIdAndSubjectOrderByUpdatedAtDesc(USER_ID, subject))
                .thenReturn(mockChats);
        when(entityMapper.toChatResponseList(mockChats)).thenReturn(mockResponses);

        // Act
        ChatsListResponse result = chatService.getUserChats(USER_ID, null, subject);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getChats().size());
        assertEquals("MATH", result.getChats().get(0).getSubject());

        verify(chatRepository, times(1))
                .findByUserIdAndSubjectOrderByUpdatedAtDesc(USER_ID, subject);
        verify(entityMapper, times(1)).toChatResponseList(mockChats);
    }

    @Test
    @DisplayName("deleteChat: должен удалить чат если пользователь владелец")
    void deleteChat_ShouldDeleteChat_WhenUserIsOwner() {
        // Arrange
        Chat chat = createMockChat(CHAT_ID, "Test");
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        // Act
        chatService.deleteChat(CHAT_ID, USER_ID);

        // Assert
        verify(chatRepository, times(1)).deleteById(CHAT_ID);
    }

    @Test
    @DisplayName("deleteChat: должен выбросить исключение если чат не найден")
    void deleteChat_ShouldThrowException_WhenChatNotFound() {
        // Arrange
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ChatNotFoundException exception = assertThrows(ChatNotFoundException.class,
                () -> chatService.deleteChat(CHAT_ID, USER_ID)
        );
        assertEquals("Chat not found", exception.getMessage());
        verify(chatRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteChat: должен выбросить UnauthorizedException если пользователь не владелец")
    void deleteChat_ShouldThrowUnauthorizedException_WhenUserNotOwner() {
        Chat chat = createMockChat(CHAT_ID, "Test");
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> chatService.deleteChat(CHAT_ID, OTHER_USER_ID)
        );
        assertTrue(exception.getMessage().contains("not authorized"));
        verify(chatRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("getChatMessages: должен вернуть сообщения чата")
    void getChatMessages_ShouldReturnMessages_WhenUserIsOwner() {
        // Arrange
        Chat chat = createMockChat(CHAT_ID, "Test");
        List<ChatMessage> messages = List.of(
                createMockMessage(1L, "Hello"),
                createMockMessage(2L, "World")
        );
        List<ChatMessageResponse> mockResponses = List.of(
                ChatMessageResponse.builder().id(1L).content("Hello").build(),
                ChatMessageResponse.builder().id(2L).content("World").build()
        );

        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderByCreatedAtAsc(CHAT_ID)).thenReturn(messages);
        when(entityMapper.toChatMessageResponseList(messages)).thenReturn(mockResponses);

        // Act
        ChatMessagesListResponse result = chatService.getChatMessages(CHAT_ID, USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getMessages().size());
        assertEquals("Hello", result.getMessages().get(0).getContent());
        assertEquals("World", result.getMessages().get(1).getContent());

        verify(messageRepository, times(1)).findByChatIdOrderByCreatedAtAsc(CHAT_ID);
        verify(entityMapper, times(1)).toChatMessageResponseList(messages);
    }

    @Test
    @DisplayName("getChatMessages: должен выбросить UnauthorizedException если пользователь не владелец")
    void getChatMessages_ShouldThrowUnauthorizedException_WhenUserNotOwner() {
        Chat chat = createMockChat(CHAT_ID, "Test");
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        assertThrows(UnauthorizedException.class,
                () -> chatService.getChatMessages(CHAT_ID, OTHER_USER_ID)
        );
    }

    @Test
    @DisplayName("addMessage: должен добавить сообщение и обновить timestamp чата")
    void addMessage_ShouldAddMessage_AndUpdateChatTimestamp() {
        // Arrange
        Chat chat = createMockChat(CHAT_ID, "Test");
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(messageRepository.countByChatIdAndRole(CHAT_ID, "user")).thenReturn(1L);
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatRepository.save(any(Chat.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ChatMessage result = chatService.addMessage(CHAT_ID, USER_ID, "Test message", "user", null);

        // Assert
        assertNotNull(result);
        assertEquals("Test message", result.getContent());
        verify(chatRepository, times(1)).save(any(Chat.class));
    }

    @Test
    @DisplayName("addMessage: должен установить title из первого сообщения пользователя")
    void addMessage_ShouldSetTitleFromFirstUserMessage() {
        // Arrange
        Chat chat = createMockChat(CHAT_ID, "New Chat");
        String firstMessage = "How to solve this math problem?";

        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(messageRepository.countByChatIdAndRole(CHAT_ID, "user")).thenReturn(0L); // Первое сообщение
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Chat> chatCaptor = ArgumentCaptor.forClass(Chat.class);
        when(chatRepository.save(chatCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        chatService.addMessage(CHAT_ID, USER_ID, firstMessage, "user", null);

        // Assert
        Chat savedChat = chatCaptor.getValue();
        assertEquals("How to solve this math problem?", savedChat.getTitle());
    }

    @Test
    @DisplayName("addMessage: не должен менять title для не-первого сообщения")
    void addMessage_ShouldNotChangeTitleForNonFirstMessage() {
        // Arrange
        Chat chat = createMockChat(CHAT_ID, "Original Title");

        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(messageRepository.countByChatIdAndRole(CHAT_ID, "user")).thenReturn(1L); // Не первое
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Chat> chatCaptor = ArgumentCaptor.forClass(Chat.class);
        when(chatRepository.save(chatCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        chatService.addMessage(CHAT_ID, USER_ID, "Second message", "user", null);

        // Assert
        Chat savedChat = chatCaptor.getValue();
        assertEquals("Original Title", savedChat.getTitle()); // Title не изменился
    }

    @Test
    @DisplayName("addMessage: должен выбросить UnauthorizedException если пользователь не владелец")
    void addMessage_ShouldThrowUnauthorizedException_WhenUserNotOwner() {
        Chat chat = createMockChat(CHAT_ID, "Test");
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        assertThrows(UnauthorizedException.class,
                () -> chatService.addMessage(CHAT_ID, OTHER_USER_ID, "Test", "user", null)
        );
    }

    @Test
    @DisplayName("updateChatTitle: должен обновить title чата")
    void updateChatTitle_ShouldUpdateTitle() {
        // Arrange
        Chat chat = createMockChat(CHAT_ID, "Old Title");
        String newTitle = "New Title";

        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));
        when(chatRepository.save(any(Chat.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Chat result = chatService.updateChatTitle(CHAT_ID, USER_ID, newTitle);

        // Assert
        assertEquals(newTitle, result.getTitle());
        verify(chatRepository, times(1)).save(any(Chat.class));
    }

    @Test
    @DisplayName("updateChatTitle: должен выбросить UnauthorizedException если пользователь не владелец")
    void updateChatTitle_ShouldThrowUnauthorizedException_WhenUserNotOwner() {
        Chat chat = createMockChat(CHAT_ID, "Old Title");
        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        assertThrows(UnauthorizedException.class,
                () -> chatService.updateChatTitle(CHAT_ID, OTHER_USER_ID, "New Title")
        );
    }

    @Test
    @DisplayName("deleteAllChats: должен удалить все чаты пользователя")
    void deleteAllChats_ShouldDeleteAllUserChats() {
        // Arrange
        List<Chat> userChats = List.of(
                createMockChat(1L, "Chat 1"),
                createMockChat(2L, "Chat 2")
        );
        when(chatRepository.findByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(userChats);

        // Act
        chatService.deleteAllChats(USER_ID);

        // Assert
        verify(chatRepository, times(1)).deleteAll(userChats);
    }

    @Test
    @DisplayName("getRecentChats: должен вернуть ограниченное количество чатов")
    void getRecentChats_ShouldReturnLimitedChats() {
        // Arrange
        List<Chat> allChats = List.of(
                createMockChat(1L, "Chat 1"),
                createMockChat(2L, "Chat 2"),
                createMockChat(3L, "Chat 3")
        );
        List<Chat> limitedChats = allChats.subList(0, 2);
        List<ChatResponse> limitedResponses = List.of(
                ChatResponse.builder().id(1L).title("Chat 1").build(),
                ChatResponse.builder().id(2L).title("Chat 2").build()
        );

        when(chatRepository.findByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(allChats);
        when(entityMapper.toChatResponseList(limitedChats)).thenReturn(limitedResponses);

        // Act
        ChatsListResponse result = chatService.getRecentChats(USER_ID, 2);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getChats().size());

        verify(chatRepository, times(1)).findByUserIdOrderByUpdatedAtDesc(USER_ID);
    }

    @Test
    @DisplayName("createChat: должен корректно обрезать title с 100+ словами")
    void createChat_ShouldTruncateVeryLongTitle() {
        // 100 слов
        String veryLongTitle = "word ".repeat(100).trim();
        when(chatRepository.save(any(Chat.class))).thenAnswer(inv -> inv.getArgument(0));

        Chat result = chatService.createChat(USER_ID, veryLongTitle, null);

        assertTrue(result.getTitle().length() <= 40);
        // Проверяем что это не просто "New Chat", а действительно обрезанный текст
        assertTrue(result.getTitle().startsWith("word"));
    }

    private Chat createMockChat(Long id, String title) {
        Chat chat = new Chat();
        chat.setId(id);
        chat.setUserId(USER_ID);
        chat.setTitle(title);
        chat.setSubject("MATH");
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());
        return chat;
    }

    private ChatMessage createMockMessage(Long id, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setChatId(CHAT_ID);
        message.setContent(content);
        message.setRole("user");
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }
}