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
public class ChatResponse implements Serializable {
    private Long id;
    private Long userId;
    private String title;
    private String subject;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
