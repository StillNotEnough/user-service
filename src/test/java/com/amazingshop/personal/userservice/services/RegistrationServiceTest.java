package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.enums.Role;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.util.validators.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {

    @Mock
    private UserServiceImpl userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserValidator userValidator;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private User testUser;

    @BeforeEach
    void setUp(){
        testUser = new User("testUser", "testPassword", "test@example.com");
    }

    @Test
    void register_shouldEncodePasswordAndSetUserRole(){
        // Arrange (подготовка)
        when(passwordEncoder.encode("testPassword")).thenReturn("encodedPassword");
        when(userService.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));

        // Act (действие)
        User registeredUser = registrationService.register(testUser);

        // Assert (проверка)
        assertEquals("encodedPassword", registeredUser.getPassword(), "Password should be encoded");
        assertEquals(Role.USER, registeredUser.getRole(), "Role should be set to USER by default");
        assertNotNull(registeredUser.getCreatedAt(), "Creation date should be set");

        // Verify (проверка вызовов)
        verify(userValidator, times(1)).validateAndThrow(testUser);
        verify(passwordEncoder, times(1)).encode("testPassword");
        verify(userService, times(1)).save(any(User.class));
    }
}
