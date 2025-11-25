package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.enums.Role;
import com.amazingshop.personal.userservice.interfaces.RegistrationService;
import com.amazingshop.personal.userservice.interfaces.UserService;
import com.amazingshop.personal.userservice.models.User;
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

    @Autowired
    public RegistrationServiceImpl(PasswordEncoder passwordEncoder,
                               UserService userService, UserValidator userValidator) {
        this.userService = userService;
        this.userValidator = userValidator;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(User user) {
        log.info("Attempting to register user: {}", user.getUsername());

        // Валидация перед сохранением
        userValidator.validateAndThrow(user);

        // Подготовка данных
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(Role.USER);

        // Сохранение
        User savedUser = userService.save(user);

        log.info("User successfully registered: {}", savedUser.getUsername());
        return savedUser;
    }
}