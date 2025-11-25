package com.amazingshop.personal.userservice.models;

import com.amazingshop.personal.userservice.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Users")
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    @NotEmpty(message = "Username should be not empty")
    @Size(min = 2, max = 30, message = "Username should be for 2 to 30 symbols")
    private String username;

    @Column(name = "password", nullable = false)
    @Size(min = 6, message = "Password should be at least 6 characters")
    private String password;

    @Column(name = "email", nullable = false)
    @NotEmpty(message = "Email should be not empty")
    @Email(message = "Email should be valid")
    private String email;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "role")
    private Role role;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(name = "refresh_token_expiry")
    private LocalDateTime refreshTokenExpiry;

    // Автоматически устанавливаем createdAt и роль по умолчанию
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.role == null) {
            this.role = Role.USER;
        }
    }

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
}
