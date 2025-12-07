package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.dto.requests.AuthenticationDTO;
import com.amazingshop.personal.userservice.dto.requests.RefreshTokenRequest;
import com.amazingshop.personal.userservice.dto.responses.TokenPairResponse;
import com.amazingshop.personal.userservice.interfaces.UserService;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.security.jwt.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    @DisplayName("performLogin: должен вернуть токены для валидных credentials")
    void performLogin_ShouldReturnTokens_WhenCredentialsValid() {
        // Arrange
        AuthenticationDTO authDTO = new AuthenticationDTO();
        authDTO.setUsername("testUser");
        authDTO.setPassword("password123");

        User user = new User();
        user.setUsername("testUser");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userService.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken("testUser")).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken("testUser")).thenReturn("refresh_token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900L);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(604800L);
        when(userService.save(any(User.class))).thenReturn(user);

        // Act
        TokenPairResponse response = authenticationService.performLogin(authDTO);

        // Assert
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("testUser", response.getUsername());

        verify(authenticationManager, times(1)).authenticate(any());
        verify(userService, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("performLogin: должен выбросить исключение для неверных credentials")
    void performLogin_ShouldThrowException_WhenCredentialsInvalid() {
        // Arrange
        AuthenticationDTO authDTO = new AuthenticationDTO();
        authDTO.setUsername("testUser");
        authDTO.setPassword("wrongPassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class,
                () -> authenticationService.performLogin(authDTO));
    }

    @Test
    @DisplayName("logout: должен удалить refresh token из БД")
    void logout_ShouldRemoveRefreshToken() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid_refresh_token");

        User user = new User();
        user.setUsername("testUser");
        user.setRefreshToken("valid_refresh_token");

        when(jwtUtil.validateTokenAndRetrieveClaim("valid_refresh_token"))
                .thenReturn("testUser");
        when(userService.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(userService.save(any(User.class))).thenReturn(user);

        // Act
        authenticationService.logout(request);

        // Assert
        verify(userService).save(argThat(u ->
                u.getRefreshToken() == null && u.getRefreshTokenExpiry() == null
        ));
    }
}