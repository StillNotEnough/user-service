package com.amazingshop.personal.userservice.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse implements Serializable {
    private Long id;
    private Long chatId;
    private String role;
    private String content;
    private String templateUsed;
    private LocalDateTime createdAt;
}