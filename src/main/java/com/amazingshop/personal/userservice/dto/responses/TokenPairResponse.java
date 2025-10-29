package com.amazingshop.personal.userservice.dto.responses;

// СОЗДАЙ новый файл: dto/responses/TokenPairResponse.java

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenPairResponse {
    private String accessToken;
    private long accessTokenExpiresIn;
    private String refreshToken;
    private long refreshTokenExpiresIn;
    private String username;
}