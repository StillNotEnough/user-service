package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.dto.responses.CurrentUserResponse;
import com.amazingshop.personal.userservice.interfaces.UserService;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.repositories.UsersRepository;
import com.amazingshop.personal.userservice.security.details.UserDetailsImpl;
import com.amazingshop.personal.userservice.util.exceptions.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UsersRepository usersRepository;
    private UserService self;

    @Autowired
    public UserServiceImpl(UsersRepository usersRepository, @Lazy UserService self) {
        this.usersRepository = usersRepository;
        this.self = self;
    }

    @Override
    @Cacheable(value = "userByUsername", key = "#username")
    public Optional<User> findByUsername(String username) {
        log.debug("Searching for user by username: {}", username);
        return usersRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        log.debug("Searching for user by email: {}", email);
        return usersRepository.findByEmail(email);
    }

    @Override
    @Cacheable(value = "userById", key = "#id")
    public User findUserByIdOrThrow(Long id) {
        return usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));
    }

    @Override
    public List<User> findAll() {
        return usersRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = "userByUsername", key = "#user.username")
    public User save(User user) {
        log.debug("Saving user: {}", user.getUsername());
        return usersRepository.save(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userById", key = "#id")
    public void deleteById(Long id) {
        log.info("Deleting user with id: {}", id);
        if (!usersRepository.existsById(id)) {
            throw new UserNotFoundException("User with id " + id + " not found");
        }
        usersRepository.deleteById(id);
    }

    @Override
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        return self.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found!"))
                .getId();
    }

    @Override
    public CurrentUserResponse getCurrentUserResponse() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = self.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        // Строим безопасный ответ
        return CurrentUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .profilePictureUrl(user.getProfilePictureUrl())
                .createdAt(user.getCreatedAt())
                //Пока всем FREE
                .subscriptionPlan("FREE")
                .subscriptionExpiresAt(null)
                .build();
    }

    @Override
    @Transactional
    public CurrentUserResponse updateCurrentUserResponse(Map<String, String> updates) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User currentUser = self.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        // Обновляем только разрешенные поля
        if (updates.containsKey("email")) {
            currentUser.setEmail(updates.get("email"));
        }

        if (updates.containsKey("profilePictureUrl")) {
            currentUser.setProfilePictureUrl(updates.get("profilePictureUrl"));
        }

        User updatedUser = self.save(currentUser);

        return CurrentUserResponse.builder()
                .id(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .role(updatedUser.getRole())
                .profilePictureUrl(updatedUser.getProfilePictureUrl())
                .createdAt(updatedUser.getCreatedAt())
                .subscriptionPlan("FREE")
                .subscriptionExpiresAt(null)
                .build();
    }
}