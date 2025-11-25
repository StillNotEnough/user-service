package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.enums.Role;
import com.amazingshop.personal.userservice.interfaces.UserService;
import com.amazingshop.personal.userservice.models.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AdminService {

    private final UserService userService;

    public AdminService(UserServiceImpl userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public String sayForAdmin() {
        return "Only admins can see this message";
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        log.info("Admin requested all users list");
        return userService.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long userId) {
        log.info("Admin requested to delete user with id: {}", userId);
        userService.deleteById(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public User promoteToAdmin(Long userId) {
        log.info("Admin requested to promote user {} to admin", userId);
        User user = userService.findUserByIdOrThrow(userId);
        user.setRole(Role.ADMIN);
        return userService.save(user);
    }
    // todo - реализовать админский контроллер
}