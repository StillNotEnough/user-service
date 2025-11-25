package com.amazingshop.personal.userservice.controllers;

import com.amazingshop.personal.userservice.dto.responses.ErrorResponse;
import com.amazingshop.personal.userservice.util.exceptions.UserNotFoundException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /// //////////////////////////////////////////////////////
    /// Validation and input data (Валидация и входные данные)
    /// //////////////////////////////////////////////////////

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handlerExceptions(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());

        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream().
                collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> Optional.ofNullable(fieldError.getDefaultMessage())
                                .orElse("Validation error occurred!"),
                        (existing, replacement) -> existing // Если дубликат поля, оставляем первую ошибку
                ));

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Validation Failed");
        errorResponse.put("message", "Input validation failed");
        errorResponse.put("fieldErrors", fieldErrors);


        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handlerHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Malformed JSON request: {}", e.getMessage());
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse("Malformed JSON request"),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handlerMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Invalid parameter type: {} for parameter: {}", e.getValue(), e.getName());
        String message = String.format("Invalid value '%s' for parameter '%s'", e.getValue(), e.getName());
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse(message),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handlerMissingRequestHeaderException(MissingRequestHeaderException e) {
        log.warn("Missing required header: {}", e.getHeaderName());
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse("Missing required header: " + e.getHeaderName()),
                HttpStatus.BAD_REQUEST);
    }

    /// ///////////////////////////////////////////////////////////////
    /// Authentication and authorization (Аутентификация и авторизация)
    /// ///////////////////////////////////////////////////////////////

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handlerValidationException(BadCredentialsException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse("Invalid username or password"),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(JWTVerificationException.class)
    public ResponseEntity<ErrorResponse> handlerJWTVerificationException(JWTVerificationException e) {
        log.warn("JWT verification failed: {}", e.getMessage());
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse("Invalid or expired JWT token"),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlerUsernameNotFoundException(UsernameNotFoundException e) {
        log.warn("User not found: {}", e.getMessage());
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse("User not found"),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handlerAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse("Invalid or expired JWT token"),
                HttpStatus.FORBIDDEN);
    }

    /// ////////////////////////////////////////////////
    /// Business logic and data (Бизнес-логика и данные)
    /// ////////////////////////////////////////////////

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlerUserNotFoundException(UserNotFoundException e) {
        log.warn("User not found: {}", e.getMessage());
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse("User not found"),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("Database constraint violation: {}", e.getMessage());
        String message = "Data integrity violation";

        // Более детальная обработка для разных типов нарушений
        if (e.getMessage().contains("duplicate") || e.getMessage().contains("unique")) {
            message = "Resource already exists";
        } else if (e.getMessage().contains("foreign key")) {
            message = "Referenced resource not found";
        }
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse(message),
                HttpStatus.CONFLICT);
    }

    /// ///////////////////////////////////////
    /// HTTP and routing (HTTP и маршрутизация)
    /// ///////////////////////////////////////

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("No handler found for {} {}", e.getHttpMethod(), e.getRequestURL());
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse("Endpoint not found"),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: {}", e.getMethod());
        String message = String.format("Method %s not supported. Supported methods: %s",
                e.getMethod(), String.join(", ", e.getSupportedMethods()));
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse(message),
                HttpStatus.METHOD_NOT_ALLOWED);
    }

    /// ////////////////////////////////
    /// System errors (Системные ошибки)
    /// ////////////////////////////////

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handlerNullPointerException(NullPointerException e) {
        log.error("Null pointer exception occurred", e);
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse("Internal server error occurred"),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse("Invalid argument: " + e.getMessage()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.error("Illegal state: {}", e.getMessage());
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse("Service temporarily unavailable"),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /// ///////////////////////////////////
    /// A common handler (Общий обработчик)
    /// ///////////////////////////////////

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handlerException(Exception e) {
        log.error("Unexpected error occurred", e);
        return new ResponseEntity<>(ErrorResponse.makeErrorResponse("An unexpected error occurred!"),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}