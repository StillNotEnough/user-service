package com.amazingshop.personal.userservice.controllers;

import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.amazingshop.personal.userservice.dto.responses.CurrentUserResponse;
import com.amazingshop.personal.userservice.dto.responses.UserResponse;
import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.security.details.UserDetailsImpl;
import com.amazingshop.personal.userservice.services.AdminService;
import com.amazingshop.personal.userservice.interfaces.ConverterService;
import com.amazingshop.personal.userservice.interfaces.UserService;
import com.amazingshop.personal.userservice.util.exceptions.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UsersController {

    private final AdminService adminService;
    private final UserService userService;
    private final ConverterService converterService;

    @Autowired
    public UsersController(AdminService adminService, UserService userService, ConverterService converterService) {
        this.adminService = adminService;
        this.userService = userService;
        this.converterService = converterService;
    }

    /**
     * Получение информации о текущем пользователе
     * GET /api/v1/users/me
     *
     * Возвращает актуальные данные пользователя из БД
     * БЕЗ чувствительной информации (пароля, refresh token)
     */
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        // Строим безопасный ответ
        CurrentUserResponse response = CurrentUserResponse.builder()
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

        log.info("User info requested for: {}", user.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Обновление информации о текущем пользователе
     * PUT /api/v1/users/me
     *
     * Позволяет обновить email и profile picture
     * Username, password, role обновляются через отдельные эндпоинты
     */
    @PutMapping("/me")
    public ResponseEntity<CurrentUserResponse> updateCurrentUser(@RequestBody Map<String, String> updates) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User currentUser = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        // Обновляем только разрешенные поля
        if (updates.containsKey("email")) {
            currentUser.setEmail(updates.get("email"));
        }

        if (updates.containsKey("profilePictureUrl")) {
            currentUser.setProfilePictureUrl(updates.get("profilePictureUrl"));
        }

        User updatedUser = userService.save(currentUser);

        CurrentUserResponse response = CurrentUserResponse.builder()
                .id(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .role(updatedUser.getRole())
                .profilePictureUrl(updatedUser.getProfilePictureUrl())
                .createdAt(updatedUser.getCreatedAt())
                .subscriptionPlan("FREE")
                .subscriptionExpiresAt(null)
                .build();

        log.info("User info updated for: {}", updatedUser.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Админский эндпоинт - приветствие
     * GET /api/v1/users/admin/hello
     */
    @GetMapping("/admin/hello")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> helloForAdmin() {
        String message = adminService.sayForAdmin();
        log.info("Admin hello endpoint accessed");
        return ResponseEntity.ok(Map.of("message", message));
    }

    /**
     * Админский эндпоинт - получение всех пользователей
     * GET /api/v1/users/admin/all
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getAllUsers() {
        List<User> users = userService.findAll();
        List<UserDTO> userDTOS = users.stream()
                .map(converterService::convertedToUserDTO)
                .toList();

        log.info("All users requested by admin, count: {}", userDTOS.size());
        return ResponseEntity.ok(new UserResponse(userDTOS));
    }

    /**
     * Админский эндпоинт - получение пользователя по ID
     * GET /api/v1/users/admin/{id}
     */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userService.findUserByIdOrThrow(id);

        UserDTO userDTO = converterService.convertedToUserDTO(user);
        log.info("User {} requested by admin", id);
        return ResponseEntity.ok(userDTO);
    }
}