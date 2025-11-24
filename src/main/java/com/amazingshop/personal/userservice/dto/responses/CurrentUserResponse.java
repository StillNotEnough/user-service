package com.amazingshop.personal.userservice.dto.responses;

import com.amazingshop.personal.userservice.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для текущего пользователя (эндпоинт /me)
 * Содержит безопасные данные пользователя БЕЗ пароля и refresh token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserResponse {

    private Long id;
    private String username;
    private String email;
    private Role role;
    private String profilePictureUrl;
    private String oauthProvider;
    private LocalDateTime createdAt;

    private String subscriptionPlan;  // "FREE", "PRO", "ENTERPRISE"
    private LocalDateTime subscriptionExpiresAt;
}