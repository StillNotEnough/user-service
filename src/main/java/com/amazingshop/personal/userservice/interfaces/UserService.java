package com.amazingshop.personal.userservice.interfaces;

import com.amazingshop.personal.userservice.models.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User findUserByIdOrThrow(Long id);
    List<User> findAll();
    User save(User user);
    void deleteById(Long id);
    Long getCurrentUserId();
}
