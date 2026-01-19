package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.amazingshop.personal.userservice.dto.responses.TokenPairResponse;
import com.amazingshop.personal.userservice.enums.Role;
import com.amazingshop.personal.userservice.interfaces.EntityMapper;
import com.amazingshop.personal.userservice.interfaces.RegistrationService;
import com.amazingshop.personal.userservice.interfaces.UserService;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.security.jwt.JwtUtil;
import com.amazingshop.personal.userservice.util.validators.UserValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional(readOnly = true)
public class RegistrationServiceImpl implements RegistrationService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;
    private final EntityMapper entityMapper;
    private final JwtUtil jwtUtil;

    @Autowired
    public RegistrationServiceImpl(PasswordEncoder passwordEncoder,
                                   UserService userService, UserValidator userValidator, EntityMapper entityMapper, JwtUtil jwtUtil) {
        this.userService = userService;
        this.userValidator = userValidator;
        this.passwordEncoder = passwordEncoder;
        this.entityMapper = entityMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public TokenPairResponse register(UserDTO userDTO) {
        log.info("Registration attempt for username: {}", userDTO.getUsername());

        User user = entityMapper.toUser(userDTO);
        User preparedUser = prepareUserForRegistration(user);
        User savedUser = userService.save(preparedUser);

        TokenPairResponse response = generateTokensAndReturnResponse(savedUser);
        log.info("User registered successfully: {}", savedUser.getUsername());
        return response;
    }

    // Private метод для подготовки пользователя
    private User prepareUserForRegistration(User user) {
        // Валидация перед сохранением
        userValidator.validateAndThrow(user);

        // Подготовка данных
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(Role.USER);

        return user;
    }

    private TokenPairResponse generateTokensAndReturnResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now()
                .plusSeconds(jwtUtil.getRefreshTokenExpiration()));
        userService.save(user); // Этот вызов тоже в той же транзакции

        return new TokenPairResponse(
                accessToken,
                jwtUtil.getAccessTokenExpiration(),
                refreshToken,
                jwtUtil.getRefreshTokenExpiration(),
                user.getUsername());
    }
}