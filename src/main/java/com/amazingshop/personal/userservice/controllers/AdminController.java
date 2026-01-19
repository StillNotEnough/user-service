package com.amazingshop.personal.userservice.controllers;

import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.amazingshop.personal.userservice.dto.responses.UserResponse;
import com.amazingshop.personal.userservice.interfaces.AdminService;
import com.amazingshop.personal.userservice.interfaces.EntityMapper;
import com.amazingshop.personal.userservice.interfaces.UserService;
import com.amazingshop.personal.userservice.models.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final AdminService adminService;
    private final EntityMapper entityMapper;

    @Autowired
    public AdminController(UserService userService, AdminService adminService, EntityMapper entityMapper) {
        this.userService = userService;
        this.adminService = adminService;
        this.entityMapper = entityMapper;
    }

    /**
     * Админский эндпоинт - приветствие
     * GET /api/v1/users/admin/hello
     */
    @GetMapping("/hello")
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
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getAllUsers() {
        List<User> users = userService.findAll();
        List<UserDTO> userDTOS = users.stream()
                .map(entityMapper::toUserDTO)
                .toList();

        log.info("All users requested by admin, count: {}", userDTOS.size());
        return ResponseEntity.ok(new UserResponse(userDTOS));
    }

    /**
     * Админский эндпоинт - получение пользователя по ID
     * GET /api/v1/users/admin/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        User user = userService.findUserByIdOrThrow(id);

        UserDTO userDTO = entityMapper.toUserDTO(user);
        log.info("User {} requested by admin", id);
        return ResponseEntity.ok(userDTO);
    }

    /**
     * Админский эндпоинт - удаление пользователя по ID
     * DELETE /api/v1/admin/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Admin requested to delete user with id: {}", id);
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Админский эндпоинт - удаление пользователя по ID
     * DELETE /api/v1/admin/{id}
     */
    @PutMapping("/{id}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> promoteToAdmin(@PathVariable Long id) {
        log.info("Admin requested to promote user with id: {} to admin", id);
        User promotedUser = adminService.promoteToAdmin(id);
        UserDTO userDTO = entityMapper.toUserDTO(promotedUser);
        return ResponseEntity.ok(userDTO);
    }
}