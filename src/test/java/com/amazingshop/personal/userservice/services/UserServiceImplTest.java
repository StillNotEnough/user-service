package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.dto.responses.CurrentUserResponse;
import com.amazingshop.personal.userservice.enums.Role;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.repositories.UsersRepository;
import com.amazingshop.personal.userservice.security.details.UserDetailsImpl;
import com.amazingshop.personal.userservice.util.exceptions.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock // не идет в бд
    private UsersRepository usersRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(usersRepository, null);
            ReflectionTestUtils.setField(userService, "self", userService);
    }

    @Test
    @DisplayName("findByUsername: должен вернуть пользователя по имени, если он существует")
    void findByUsername_ShouldReturnUser_WhenExists() {
        // Arrange
        String username = "testUser";
        User mockUser = new User();
        mockUser.setUsername(username);

        when(usersRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        // Act
        Optional<User> result = userService.findByUsername(username);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());

        verify(usersRepository, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("findByUsername: должен вернуть Optional.empty(), если пользователь не найден")
    void findByUsername_ShouldReturnEmpty_WhenNotExists() {
        String username = "nonExistent";

        when(usersRepository.findByUsername(username)).thenReturn(Optional.empty());

        Optional<User> result = userService.findByUsername(username);

        assertFalse(result.isPresent());
        verify(usersRepository, times(1)).findByUsername(username);
    }

    @Test
    @DisplayName("findByEmail: должен вернуть пользователя по email, если он существует")
    void findByEmail_ShouldReturnUser_WhenExists() {
        // Array
        String email = "test@gmail.com";
        User mockUser = new User();
        mockUser.setEmail(email);

        when(usersRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        // Act
        Optional<User> result = userService.findByEmail(email);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());

        verify(usersRepository, times(1)).findByEmail(email);

    }

    @Test
    @DisplayName("findUserByIdOrThrow: если id нет - то должно быть исключение")
    void findUserByIdOrThrow_ShouldThrowException_WhenNotFound() {
        Long id = 999L;

        when(usersRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findUserByIdOrThrow(id));
    }

    @Test
    @DisplayName("findAll: должен вернуть всех пользователей, если они есть")
    void findAll_ShouldReturnAllUsers_WhenExists() {
        // Arrange
        User mockUser1 = new User();
        mockUser1.setUsername("Bob");
        User mockUser2 = new User();
        mockUser2.setUsername("Alex");

        when(usersRepository.findAll()).thenReturn(List.of(mockUser1, mockUser2));

        // Act
        List<User> result = userService.findAll();

        // Assert
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());

        verify(usersRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("save: должен сохранить и вернуть пользователя")
    void save_ShouldReturnSavedUser() {
        User user = new User();
        user.setUsername("newUser");

        when(usersRepository.save(user)).thenReturn(user);

        User saved = userService.save(user);

        assertNotNull(saved);
        assertEquals("newUser", saved.getUsername());

        verify(usersRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("deleteById: должен удалить пользователя, если он существует")
    void deleteById_ShouldDelete_WhenExists() {
        Long id = 1L;

        when(usersRepository.existsById(id)).thenReturn(true);

        userService.deleteById(id);

        verify(usersRepository, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("deleteById: должен выбросить исключение, если пользователь не существует")
    void deleteById_ShouldThrowException_WhenNotExists() {
        Long id = 999L;

        when(usersRepository.existsById(id)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> userService.deleteById(id));
        verify(usersRepository, never()).deleteById(id);
    }

    @Test
    @DisplayName("getCurrentUserId: должен достать id из контекста безопасности")
    void getCurrentUserId_ShouldReturnId() {
        // Arrange
        String username = "authUser";
        Long userId = 10L;

        User dbUser = new User();
        dbUser.setId(userId);
        dbUser.setUsername(username);

        when(usersRepository.findByUsername(username)).thenReturn(Optional.of(dbUser));

        SecurityContext securityContext = mockSecurityContext(username);

        // Статический мок
        try (MockedStatic<SecurityContextHolder> mockedSecurity = Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurity.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // Act
            Long resultId = userService.getCurrentUserId();

            // Assert
            assertEquals(userId, resultId);
        }
    }

    @Test
    @DisplayName("getCurrentUserId: должен выбросить исключение, если пользователь не найден в БД")
    void getCurrentUserId_ShouldThrowException_WhenUserNotFoundInDb() {
        String username = "unknownUser";

        when(usersRepository.findByUsername(username)).thenReturn(Optional.empty());

        SecurityContext securityContext = mockSecurityContext(username);

        try (MockedStatic<SecurityContextHolder> mockedSecurity = Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurity.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            assertThrows(UserNotFoundException.class, () -> userService.getCurrentUserId());
        }
    }

    @Test
    @DisplayName("getCurrentUserResponse: должен вернуть DTO текущего пользователя")
    void getCurrentUserResponse_ShouldReturnDto() {
        String username = "me";

        User dbUser = new User();
        dbUser.setId(5L);
        dbUser.setUsername(username);
        dbUser.setEmail("me@mail.com");
        dbUser.setRole(Role.USER);

        when(usersRepository.findByUsername(username)).thenReturn(Optional.of(dbUser));

        SecurityContext securityContext = mockSecurityContext(username);

        try (MockedStatic<SecurityContextHolder> mockedSecurity = Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurity.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            CurrentUserResponse response = userService.getCurrentUserResponse();

            assertNotNull(response);
            assertEquals(username, response.getUsername());
            assertEquals("me@mail.com", response.getEmail());
            assertEquals(Role.USER, response.getRole());
            assertEquals("FREE", response.getSubscriptionPlan());

        }
    }

    @Test
    @DisplayName("updateCurrentUserResponse: должен обновить поля и вернуть DTO")
    void updateCurrentUserResponse_ShouldUpdateAndReturnDto() {
        String username = "updatableUser";
        String newEmail = "new@mail.com";

        User dbUser = new User();
        dbUser.setId(1L);
        dbUser.setUsername(username);
        dbUser.setEmail("old@mail.com");
        dbUser.setRole(Role.ADMIN);


        when(usersRepository.findByUsername(username)).thenReturn(Optional.of(dbUser));
        // Эмулируем, что save возвращает обновленный объект
        when(usersRepository.save(any(User.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        SecurityContext securityContext = mockSecurityContext(username);

        try (MockedStatic<SecurityContextHolder> mockedSecurity = Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurity.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // Act
            CurrentUserResponse response = userService.updateCurrentUserResponse(Map.of("email", newEmail));

            // Assert
            assertEquals(newEmail, response.getEmail());
            verify(usersRepository).save(argThat(u -> u.getEmail().equals(newEmail)));
        }
    }

    private SecurityContext mockSecurityContext(String username) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(username);

        return securityContext;
    }
}