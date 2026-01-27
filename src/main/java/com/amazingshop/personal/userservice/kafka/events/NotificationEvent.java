package com.amazingshop.personal.userservice.kafka.events;

import com.amazingshop.personal.userservice.models.User;

import java.time.Instant;
import java.util.Map;

public record NotificationEvent(
        String type,
        String userId,
        String email,
        Map<String, Object> data,
        Instant timestamp
) {
    public static NotificationEvent welcome (User user){
        return new NotificationEvent(
                "WELCOME",
                user.getId().toString(),
                user.getEmail(),
                Map.of("username", user.getUsername()),
                Instant.now()
        );
    }
}