package com.amazingshop.personal.userservice;

import com.amazingshop.personal.userservice.interfaces.UserService;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.util.exceptions.UserValidationException;
import com.amazingshop.personal.userservice.util.validators.UserValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserValidatorTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserValidator userValidator;

    @Test
    @DisplayName("validateUsername: должен пройти для валидного username")
    void validateUsername_ShouldPass_WhenValid() {
        // Arrange
        String validUsername = "testUser";
        when(userService.findByUsername(validUsername)).thenReturn(Optional.empty());

        // Act & Assert
        assertDoesNotThrow(() -> userValidator.validateUsername(validUsername));
        Mockito.verify(userService, times(1)).findByUsername(validUsername);
    }

    @Test
    @DisplayName("validateUsername: должен выбросить исключение для пустого username")
    void validateUsername_ShouldThrowException_WhenEmpty() {
        // Act & Assert
        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> userValidator.validateUsername("")
        );
        assertEquals("Username cannot be empty!", exception.getMessage());
    }

    @Test
    @DisplayName("validateUsername: должен выбросить исключение для null username")
    void validateUsername_ShouldThrowException_WhenNull(){
        UserValidationException exception = assertThrows(
                UserValidationException.class, () ->
                        userValidator.validateUsername(null)
        );
        assertEquals("Username cannot be empty!", exception.getMessage());
    }

    @Test
    @DisplayName("validateUsername: должен выбросить исключение для слишком короткого username")
    void validateUsername_ShouldThrowException_WhenTooShort() {
        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> userValidator.validateUsername("a")
        );
        assertEquals("Username should be between 2 and 30 characters!", exception.getMessage());
    }

    @Test
    @DisplayName("validateUsername: должен выбросить исключение для слишком длинного username")
    void validateUsername_ShouldThrowException_WhenTooLong() {
        String longUsername = "a".repeat(31);

        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> userValidator.validateUsername(longUsername)
        );
        assertEquals("Username should be between 2 and 30 characters!", exception.getMessage());
    }

    @Test
    @DisplayName("validateUsername: должен выбросить исключение если username уже существует")
    void validateUsername_ShouldThrowException_WhenAlreadyExists() {
        // Arrange
        String existingUsername = "existingUser";
        User existingUser = new User();
        existingUser.setUsername(existingUsername);

        when(userService.findByUsername(existingUsername)).thenReturn(Optional.of(existingUser));

        // Act & Assert
        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> userValidator.validateUsername(existingUsername)
        );
        assertEquals("A user with this username already exists!", exception.getMessage());
    }

    @Test
    @DisplayName("validateEmail: должен пройти для валидного email")
    void validateEmail_ShouldPass_WhenValid() {
        // Arrange
        String validEmail = "test@example.com";
        when(userService.findByEmail(validEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertDoesNotThrow(() -> userValidator.validateEmail(validEmail));
        verify(userService, times(1)).findByEmail(validEmail);
    }

    @Test
    @DisplayName("validateEmail: должен выбросить исключение для пустого email")
    void validateEmail_ShouldThrowException_WhenEmpty() {
        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> userValidator.validateEmail("")
        );
        assertEquals("Email cannot be empty!", exception.getMessage());
    }

    @Test
    @DisplayName("validateEmail: должен выбросить исключение для невалидного формата email")
    void validateEmail_ShouldThrowException_WhenInvalidFormat() {
        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> userValidator.validateEmail("invalid-email")
        );
        assertEquals("Invalid email format!", exception.getMessage());
    }

    @Test
    @DisplayName("validateEmail: должен выбросить исключение если email уже существует")
    void validateEmail_ShouldThrowException_WhenAlreadyExists() {
        // Arrange
        String existingEmail = "existing@example.com";
        User existingUser = new User();
        existingUser.setEmail(existingEmail);

        when(userService.findByEmail(existingEmail)).thenReturn(Optional.of(existingUser));

        // Act & Assert
        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> userValidator.validateEmail(existingEmail)
        );
        assertEquals("A user with this email already exists!", exception.getMessage());
    }

    @Test
    @DisplayName("validatePassword: должен пройти для валидного password")
    void validatePassword_ShouldPass_WhenValid() {
        assertDoesNotThrow(() -> userValidator.validatePassword("validPass123"));
    }

    @Test
    @DisplayName("validatePassword: должен выбросить исключение для пустого password")
    void validatePassword_ShouldThrowException_WhenEmpty() {
        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> userValidator.validatePassword("")
        );
        assertEquals("Password cannot be empty!", exception.getMessage());
    }

    @Test
    @DisplayName("validatePassword: должен выбросить исключение для слишком короткого password")
    void validatePassword_ShouldThrowException_WhenTooShort() {
        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> userValidator.validatePassword("12345")
        );
        assertEquals("Password should be at least 6 characters long!", exception.getMessage());
    }

    @Test
    @DisplayName("validateAndThrow: должен пройти для полностью валидного пользователя")
    void validateAndThrow_ShouldPass_WhenAllFieldsValid() {
        // Arrange
        User validUser = new User();
        validUser.setUsername("validUser");
        validUser.setEmail("valid@example.com");
        validUser.setPassword("password123");

        when(userService.findByUsername("validUser")).thenReturn(Optional.empty());
        when(userService.findByEmail("valid@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertDoesNotThrow(() -> userValidator.validateAndThrow(validUser));

        verify(userService, times(1)).findByUsername("validUser");
        verify(userService, times(1)).findByEmail("valid@example.com");
    }

    @Test
    @DisplayName("validateAndThrow: должен выбросить исключение при невалидном username")
    void validateAndThrow_ShouldThrowException_WhenInvalidUsername() {
        // Arrange
        User user = new User();
        user.setUsername("a"); // Слишком короткий
        user.setEmail("valid@example.com");
        user.setPassword("password123");

        // Act & Assert
        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> userValidator.validateAndThrow(user)
        );
        assertEquals("Username should be between 2 and 30 characters!", exception.getMessage());
    }

    @Test
    @DisplayName("validateAndThrow: должен выбросить исключение при невалидном email")
    void validateAndThrow_ShouldThrowException_WhenInvalidEmail() {
        // Arrange
        User user = new User();
        user.setUsername("validUser");
        user.setEmail("invalid-email"); // Невалидный формат
        user.setPassword("password123");

        when(userService.findByUsername("validUser")).thenReturn(Optional.empty());

        // Act & Assert
        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> userValidator.validateAndThrow(user)
        );
        assertEquals("Invalid email format!", exception.getMessage());
    }

    @Test
    @DisplayName("validateAndThrow: должен выбросить исключение при невалидном password")
    void validateAndThrow_ShouldThrowException_WhenInvalidPassword() {
        // Arrange
        User user = new User();
        user.setUsername("validUser");
        user.setEmail("valid@example.com");
        user.setPassword("123"); // Слишком короткий

        when(userService.findByUsername("validUser")).thenReturn(Optional.empty());
        when(userService.findByEmail("valid@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        UserValidationException exception = assertThrows(
                UserValidationException.class,
                () -> userValidator.validateAndThrow(user)
        );
        assertEquals("Password should be at least 6 characters long!", exception.getMessage());
    }

    @Test
    @DisplayName("supports: должен вернуть true для класса User")
    void supports_ShouldReturnTrue_ForUserClass() {
        assertTrue(userValidator.supports(User.class));
    }

    @Test
    @DisplayName("supports: должен вернуть false для других классов")
    void supports_ShouldReturnFalse_ForOtherClasses() {
        assertFalse(userValidator.supports(String.class));
        assertFalse(userValidator.supports(Object.class));
    }
}