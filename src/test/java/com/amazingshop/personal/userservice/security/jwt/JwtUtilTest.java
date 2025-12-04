package com.amazingshop.personal.userservice.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Date;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JwtUtilTest {

    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_SECRET = "mySuperSecretForTesting123456789012345678901234567890";
    private static final long ACCESS_EXPIRATION_MS = 10000; // 10 сек
    private static final long REFRESH_EXPIRATION_MS = 20000; // 20 сек

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(); // Создаем экземпляр
        // как будто @Value сработал, через ReflectionTestUtils
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", ACCESS_EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", REFRESH_EXPIRATION_MS);
    }

    @Test
    @DisplayName("generateAccessToken: должен вернуть валидный access токен")
    void generateAccessToken_ShouldReturnValidToken() {
        // Act
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);

        // Assert
        assertNotNull(token);
        assertFalse(token.isBlank());

        // Проверим, что токен валидный и содержит нужные поля
        Algorithm algorithm = Algorithm.HMAC256(TEST_SECRET);
        var verifier = JWT.require(algorithm)
                .withSubject("User details")
                .withIssuer("ShporaAi")
                .withClaim("username", TEST_USERNAME)
                .withClaim("type", "access")
                .build();

        // Проверка не выбросит исключение, если токен валиден
        var decodedJWT = verifier.verify(token);
        assertEquals(TEST_USERNAME, decodedJWT.getClaim("username").asString());
        assertEquals("access", decodedJWT.getClaim("type").asString());
    }

    @Test
    @DisplayName("generateRefreshToken: должен вернуть валидный refresh токен")
    void generateRefreshToken_ShouldReturnValidToken() {
        // Act
        String token = jwtUtil.generateRefreshToken(TEST_USERNAME);

        // Assert
        assertNotNull(token);
        assertFalse(token.isBlank());

        // Проверим, что токен валидный и содержит нужные поля
        Algorithm algorithm = Algorithm.HMAC256(TEST_SECRET);
        var verifier = JWT.require(algorithm)
                .withSubject("User details")
                .withIssuer("ShporaAi")
                .withClaim("username", TEST_USERNAME)
                .withClaim("type", "refresh")
                .build();

        var decodedJWT = verifier.verify(token);
        assertEquals(TEST_USERNAME, decodedJWT.getClaim("username").asString());
        assertEquals("refresh", decodedJWT.getClaim("type").asString());
        assertNotNull(decodedJWT.getClaim("jti").asString()); // jti должен быть
    }

    @Test
    @DisplayName("generateRefreshToken: должен содержать уникальный jti")
    void generateRefreshToken_ShouldHaveUniqueJti() {
        // Act
        String token1 = jwtUtil.generateRefreshToken(TEST_USERNAME);
        String token2 = jwtUtil.generateRefreshToken(TEST_USERNAME);

        var decoded1 = JWT.decode(token1);
        var decoded2 = JWT.decode(token2);

        // Assert
        assertNotEquals(decoded1.getClaim("jti").asString(),
                decoded2.getClaim("jti").asString(),
                "jti должен быть уникальным для каждого refresh токена");
    }

    @Test
    @DisplayName("validateTokenAndRetrieveClaim: должен извлечь username из валидного токена")
    void validateTokenAndRetrieveClaim_ShouldExtractUsername(){
        // Arrange
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);

        // Act
        String extractedUsername = jwtUtil.validateTokenAndRetrieveClaim(token);

        // Assert
        assertEquals(TEST_USERNAME, extractedUsername);
    }

    @Test
    @DisplayName("validateTokenAndRetrieveClaim: должен выбросить исключение для невалидной подписи")
    void validateTokenAndRetrieveClaim_ShouldThrowException_WhenInvalidSignature() {
        // Arrange - создаём токен с другим секретом
        String wrongSecret = "wrongSecret123456789012345678901234567890";
        String token = JWT.create()
                .withSubject("User details")
                .withClaim("username", TEST_USERNAME)
                .withClaim("type", "access")
                .withIssuer("ShporaAi")
                .withExpiresAt(Date.from(ZonedDateTime.now().plusMinutes(10).toInstant()))
                .sign(Algorithm.HMAC256(wrongSecret));

        // Act & Assert
        assertThrows(SignatureVerificationException.class,
                () -> jwtUtil.validateTokenAndRetrieveClaim(token),
                "Должно выброситься исключение из-за неверной подписи");
    }

    @Test
    @DisplayName("validateTokenAndRetrieveClaim: должен выбросить исключение для истекшего токена")
    void validateTokenAndRetrieveClaim_ShouldThrowException_WhenTokenExpired() {
        // Arrange - создаём токен с истекшим сроком
        String expiredToken = JWT.create()
                .withSubject("User details")
                .withClaim("username", TEST_USERNAME)
                .withClaim("type", "access")
                .withIssuer("ShporaAi")
                .withExpiresAt(Date.from(ZonedDateTime.now().minusMinutes(10).toInstant())) // -10 минут назад
                .sign(Algorithm.HMAC256(TEST_SECRET));

        // Act & Assert
        assertThrows(TokenExpiredException.class,
                () -> jwtUtil.validateTokenAndRetrieveClaim(expiredToken),
                "Должно выброситься исключение для истекшего токена");
    }

    @Test
    @DisplayName("validateTokenAndRetrieveClaim: должен выбросить исключение для токена с неверным issuer")
    void validateTokenAndRetrieveClaim_ShouldThrowException_WhenInvalidIssuer() {
        // Arrange - токен с неправильным issuer
        String tokenWithWrongIssuer = JWT.create()
                .withSubject("User details")
                .withClaim("username", TEST_USERNAME)
                .withClaim("type", "access")
                .withIssuer("WrongIssuer")
                .withExpiresAt(Date.from(ZonedDateTime.now().plusMinutes(10).toInstant()))
                .sign(Algorithm.HMAC256(TEST_SECRET));

        // Act & Assert
        assertThrows(JWTVerificationException.class,
                () -> jwtUtil.validateTokenAndRetrieveClaim(tokenWithWrongIssuer));
    }

    @Test
    @DisplayName("validateTokenAndRetrieveClaim: должен выбросить исключение для malformed токена")
    void validateTokenAndRetrieveClaim_ShouldThrowException_WhenMalformedToken() {
        // Arrange
        String malformedToken = "not.a.valid.jwt.token";

        // Act & Assert
        assertThrows(JWTVerificationException.class,
                () -> jwtUtil.validateTokenAndRetrieveClaim(malformedToken),
                "Должно выброситься исключение для некорректного формата токена");
    }

    @Test
    @DisplayName("getTokenType: должен вернуть 'access' для access токена")
    void getTokenType_ShouldReturnAccess_ForAccessToken() {
        // Arrange
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);

        // Act
        String tokenType = jwtUtil.getTokenType(token);

        // Assert
        assertEquals("access", tokenType);
    }

    @Test
    @DisplayName("getTokenType: должен вернуть 'refresh' для refresh токена")
    void getTokenType_ShouldReturnRefresh_ForRefreshToken() {
        // Arrange
        String token = jwtUtil.generateRefreshToken(TEST_USERNAME);

        // Act
        String tokenType = jwtUtil.getTokenType(token);

        // Assert
        assertEquals("refresh", tokenType);
    }

    @Test
    @DisplayName("getAccessTokenExpiration: должен вернуть правильное время в секундах")
    void getAccessTokenExpiration_ShouldReturnCorrectValue() {
        // Act
        long expiration = jwtUtil.getAccessTokenExpiration();

        // Assert
        assertEquals(ACCESS_EXPIRATION_MS / 1000, expiration,
                "Должно вернуть время истечения access токена в секундах");
    }

    @Test
    @DisplayName("getRefreshTokenExpiration: должен вернуть правильное время в секундах")
    void getRefreshTokenExpiration_ShouldReturnCorrectValue() {
        // Act
        long expiration = jwtUtil.getRefreshTokenExpiration();

        // Assert
        assertEquals(REFRESH_EXPIRATION_MS / 1000, expiration,
                "Должно вернуть время истечения refresh токена в секундах");
    }

    @Test
    @DisplayName("generateAccessToken: токен должен истечь через заданное время")
    void generateAccessToken_ShouldExpireAtCorrectTime() throws InterruptedException {
        // Arrange, короткое время жизни - (1 секунда)
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 1000L);

        // Act
        String token = jwtUtil.generateAccessToken(TEST_USERNAME);

        // Токен валиден сразу после создания
        assertDoesNotThrow(() -> jwtUtil.validateTokenAndRetrieveClaim(token));

        // Wait for token to expire
        Thread.sleep(1500); // Ждём 1.5 секунды

        // Assert
        assertThrows(TokenExpiredException.class,
                () -> jwtUtil.validateTokenAndRetrieveClaim(token),
                "Токен должен истечь через 1 секунду");
    }

    @Test
    @DisplayName("validateTokenAndRetrieveClaim: должен работать с токеном, содержащим спецсимволы в username")
    void validateToken_ShouldWork_WithSpecialCharactersInUsername() {
        // Arrange
        String usernameWithSpecialChars = "test.user+123@example";
        String token = jwtUtil.generateAccessToken(usernameWithSpecialChars);

        // Act
        String extractedUsername = jwtUtil.validateTokenAndRetrieveClaim(token);

        // Assert
        assertEquals(usernameWithSpecialChars, extractedUsername);
    }

    @Test
    @DisplayName("validateTokenAndRetrieveClaim: должен выбросить исключение для пустого токена")
    void validateToken_ShouldThrowException_WhenEmptyToken() {
        // Act & Assert
        assertThrows(JWTVerificationException.class,
                () -> jwtUtil.validateTokenAndRetrieveClaim(""));
    }

    @Test
    @DisplayName("validateTokenAndRetrieveClaim: должен выбросить исключение для null токена")
    void validateToken_ShouldThrowException_WhenNullToken() {
        // Act & Assert
        assertThrows(Exception.class,
                () -> jwtUtil.validateTokenAndRetrieveClaim(null));
    }
}