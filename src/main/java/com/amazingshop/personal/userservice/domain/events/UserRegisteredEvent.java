package com.amazingshop.personal.userservice.domain.events;

import com.amazingshop.personal.userservice.models.User;

// domain event
public record UserRegisteredEvent(
        User user
) {}
