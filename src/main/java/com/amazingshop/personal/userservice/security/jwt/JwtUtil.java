package com.amazingshop.personal.userservice.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@Getter
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // Access token - короткий (15-30 минут)
    @Value("${jwt.access-token-expiration:1800000}") // по умолчанию 30 минут
    private long accessTokenExpiration;

    // Refresh token - длинный (2 недели)
    @Value("${jwt.refresh-token-expiration:1209600000}") // по умолчанию 14 дней
    private long refreshTokenExpiration;


    /**
     * Генерация Access Token
     */
    public String generateAccessToken(String username){
        log.debug("Generating JWT token for user: {}", username);
        Date expirationDate = Date.from(ZonedDateTime.now()
                .plusSeconds(accessTokenExpiration / 1000)
                .toInstant());

        return JWT.create()
                .withSubject("User details")
                .withClaim("username", username)
                .withClaim("type", "access")
                .withIssuedAt(new Date())
                .withIssuer("NoNameAI")
                .withExpiresAt(expirationDate)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * Генерация Refresh Token
     */
    public String generateRefreshToken(String username) {
        Date expirationDate = Date.from(ZonedDateTime.now()
                .plusSeconds(refreshTokenExpiration / 1000)
                .toInstant());

        return JWT.create()
                .withSubject("User details")
                .withClaim("username", username)
                .withClaim("type", "refresh")
                .withClaim("jti", UUID.randomUUID().toString()) // Уникальный ID
                .withIssuedAt(new Date())
                .withIssuer("NoNameAI")
                .withExpiresAt(expirationDate)
                .sign(Algorithm.HMAC256(secret));
    }

    /**
     * Валидация токена и извлечение username
     */
    public String validateTokenAndRetrieveClaim(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                    .withSubject("User details")
                    .withIssuer("NoNameAI")
                    .build();

            DecodedJWT jwt = verifier.verify(token);
            String username = jwt.getClaim("username").asString();
            log.debug("✅ Valid JWT for user: {}", username);
            return username;

        } catch (TokenExpiredException e) {
            log.warn("⚠️ Token expired: {}", e.getMessage());
            throw e;
        } catch (SignatureVerificationException e) {
            log.error("❌ Invalid signature: {}", e.getMessage());
            throw e;
        } catch (JWTVerificationException e) {
            log.warn("JWT verification failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Проверка типа токена
     */
    public String getTokenType(String token) {
        DecodedJWT jwt = JWT.decode(token);
        return jwt.getClaim("type").asString();
    }

    /**
     * Получить время истечения access token в секундах
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration / 1000;
    }

    /**
     * Получить время истечения refresh token в секундах
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration / 1000;
    }

    /**
     * Проверка истек ли токен
     */
    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getExpiresAt().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
