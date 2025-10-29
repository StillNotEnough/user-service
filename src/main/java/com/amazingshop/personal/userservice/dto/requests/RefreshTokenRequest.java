package com.amazingshop.personal.userservice.dto.requests;

// СОЗДАЙ новый файл: dto/requests/RefreshTokenRequest.java


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}