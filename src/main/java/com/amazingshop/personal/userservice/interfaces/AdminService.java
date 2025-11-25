package com.amazingshop.personal.userservice.interfaces;

import com.amazingshop.personal.userservice.models.User;

import java.util.List;

public interface AdminService {
    String sayForAdmin();
    List<User> getAllUsers();
    void deleteUser();
    User promoteToAdmin(Long userId);
}
