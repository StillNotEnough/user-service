package com.amazingshop.personal.userservice.controllers;


import com.amazingshop.personal.userservice.dto.requests.AuthenticationDTO;
import com.amazingshop.personal.userservice.dto.requests.OAuth2LoginRequest;
import com.amazingshop.personal.userservice.dto.requests.RefreshTokenRequest;
import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.amazingshop.personal.userservice.dto.responses.OAuth2UserInfo;
import com.amazingshop.personal.userservice.dto.responses.TokenPairResponse;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.security.jwt.JwtUtil;
import com.amazingshop.personal.userservice.services.ConverterService;
import com.amazingshop.personal.userservice.services.OAuth2Service;
import com.amazingshop.personal.userservice.services.RegistrationService;
import com.amazingshop.personal.userservice.dto.responses.JwtResponse;
import com.amazingshop.personal.userservice.services.UserService;
import com.amazingshop.personal.userservice.util.validators.UserValidator;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final UserValidator userValidator;
    private final RegistrationService registrationService;
    private final JwtUtil jwtUtil;
    private final ConverterService converterService;
    private final AuthenticationManager authenticationManager;
    private final OAuth2Service oAuth2Service;
    private final UserService userService;

    @Autowired
    public AuthController(UserValidator userValidator, RegistrationService registrationService,
                          JwtUtil jwtUtil, ConverterService converterService, AuthenticationManager authenticationManager,
                          OAuth2Service oAuth2Service, UserService userService) {
        this.userValidator = userValidator;
        this.registrationService = registrationService;
        this.jwtUtil = jwtUtil;
        this.converterService = converterService;
        this.authenticationManager = authenticationManager;
        this.oAuth2Service = oAuth2Service;
        this.userService = userService;
    }

    /**
     * Регистрация нового пользователя
     * POST /api/v1/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<TokenPairResponse> performRegistration(@RequestBody @Valid UserDTO userDTO) {
        log.info("Registration attempt for username: {}", userDTO.getUsername());

        User user = converterService.convertedToPerson(userDTO);
        userValidator.validateAndThrow(user);
        registrationService.register(user);

        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now()
                .plusSeconds(jwtUtil.getRefreshTokenExpiration()));
        userService.save(user);

        log.info("User registered successfully: {}", user.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TokenPairResponse(
                        accessToken,
                        jwtUtil.getAccessTokenExpiration(),
                        refreshToken,
                        jwtUtil.getRefreshTokenExpiration(),
                        user.getUsername()
                ));
    }

    /**
     * Вход в существующий аккаунт
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<TokenPairResponse> performLogin(@RequestBody @Valid AuthenticationDTO authenticationDTO) {
        log.info("Login attempt for username: {}", authenticationDTO.getUsername());

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                authenticationDTO.getUsername(),
                authenticationDTO.getPassword()));

        User user = userService.findPersonByPersonName(authenticationDTO.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String accessToken = jwtUtil.generateAccessToken(authenticationDTO.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(authenticationDTO.getUsername());

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now()
                .plusSeconds(jwtUtil.getRefreshTokenExpiration()));
        userService.save(user);

        log.info("User logged in successfully: {}", authenticationDTO.getUsername());

        return ResponseEntity.ok(new TokenPairResponse(
                accessToken,
                jwtUtil.getAccessTokenExpiration(),
                refreshToken,
                jwtUtil.getRefreshTokenExpiration(),
                authenticationDTO.getUsername()
        ));
    }

    /**
     * Обновление access token с помощью refresh token
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        log.info("Token refresh attempt");

        try {
            // Валидируем refresh token
            String username = jwtUtil.validateTokenAndRetrieveClaim(request.getRefreshToken());

            // Проверяем тип токена
            String tokenType = jwtUtil.getTokenType(request.getRefreshToken());
            if (!"refresh".equals(tokenType)) {
                log.warn("Invalid token type for refresh: {}", tokenType);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Находим пользователя
            User user = userService.findPersonByPersonName(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Проверяем совпадает ли refresh token с сохраненным в БД
            if (!request.getRefreshToken().equals(user.getRefreshToken())) {
                log.warn("Refresh token mismatch for user: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Проверяем не истек ли refresh token
            if (user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
                log.warn("Refresh token expired for user: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Генерируем новую пару токенов
            String newAccessToken = jwtUtil.generateAccessToken(username);
            String newRefreshToken = jwtUtil.generateRefreshToken(username);

            // Обновляем refresh token в БД
            user.setRefreshToken(newRefreshToken);
            user.setRefreshTokenExpiry(LocalDateTime.now()
                    .plusSeconds(jwtUtil.getRefreshTokenExpiration()));
            userService.save(user);

            log.info("Tokens refreshed successfully for user: {}", username);

            return ResponseEntity.ok(new TokenPairResponse(
                    newAccessToken,
                    jwtUtil.getAccessTokenExpiration(),
                    newRefreshToken,
                    jwtUtil.getRefreshTokenExpiration(),
                    username
            ));

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Logout - инвалидация refresh token
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshTokenRequest request) {
        try {
            String username = jwtUtil.validateTokenAndRetrieveClaim(request.getRefreshToken());

            User user = userService.findPersonByPersonName(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Удаляем refresh token из БД
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userService.save(user);

            log.info("User logged out successfully: {}", username);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * OAuth2 Login через Google
     * POST /api/v1/auth/oauth2/google
     *//*
    @PostMapping("/oauth2/google")
    public ResponseEntity<JwtResponse> loginWithGoogle(@RequestBody @Valid OAuth2LoginRequest request) {
        log.info("Google OAuth2 login attempt");

        try {
            // Верифицируем Google ID Token
            OAuth2UserInfo userInfo = oAuth2Service.verifyGoogleToken(request.getIdToken());

            // Находим или создаем пользователя
            User user = oAuth2Service.findOrCreateOAuth2User(userInfo);

            // Генерируем JWT токен
            String token = jwtUtil.generateToken(user.getUsername());
            long expiresIn = jwtUtil.getExpirationTime();

            log.info("Google OAuth2 login successful for: {}", user.getUsername());
            return ResponseEntity.ok(new JwtResponse(token, expiresIn, user.getUsername()));

        } catch (Exception e) {
            log.error("Google OAuth2 login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }*/

    /**
     * Проверка работоспособности сервиса
     * GET /api/v1/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Auth service is running");
    }
}