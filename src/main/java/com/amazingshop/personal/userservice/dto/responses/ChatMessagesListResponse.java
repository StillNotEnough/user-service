package com.amazingshop.personal.userservice.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessagesListResponse {
    private List<ChatMessageResponse> messages;
}