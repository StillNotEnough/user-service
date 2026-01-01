package com.amazingshop.personal.userservice.controllers;


import com.amazingshop.personal.userservice.dto.requests.AuthenticationDTO;
import com.amazingshop.personal.userservice.dto.requests.RefreshTokenRequest;
import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.amazingshop.personal.userservice.dto.responses.TokenPairResponse;
import com.amazingshop.personal.userservice.interfaces.AuthenticationService;
import com.amazingshop.personal.userservice.interfaces.RegistrationService;
import com.amazingshop.personal.userservice.interfaces.TokenService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private final TokenService tokenService;

    @Autowired
    public AuthController(RegistrationService registrationService,
                          AuthenticationService authenticationService,
                          TokenService tokenService) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
        this.tokenService = tokenService;
    }

    /**
     * Регистрация нового пользователя
     * POST /api/v1/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<TokenPairResponse> performRegistration(@RequestBody @Valid UserDTO userDTO) {
        log.info("Registration attempt for username: {}", userDTO.getUsername());

        TokenPairResponse response = registrationService.register(userDTO);

        log.info("User registered successfully: {}", userDTO.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Вход в существующий аккаунт
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<TokenPairResponse> performLogin(@RequestBody @Valid AuthenticationDTO authenticationDTO) {
        log.info("Login attempt for username: {}", authenticationDTO.getUsername());

        TokenPairResponse response = authenticationService.performLogin(authenticationDTO);

        log.info("User logged in successfully: {}", authenticationDTO.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * Обновление access token с помощью refresh token
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        log.info("Token refresh attempt");

        try {
            TokenPairResponse response = tokenService.refreshToken(request);
            log.info("Tokens refreshed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) { // TODO: TokenRefreshException
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
            authenticationService.logout(request);

            log.info("Logout request processed successfully");
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}