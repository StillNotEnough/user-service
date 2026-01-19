package com.amazingshop.personal.userservice.interfaces;

import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.amazingshop.personal.userservice.dto.responses.ChatMessageResponse;
import com.amazingshop.personal.userservice.dto.responses.ChatResponse;
import com.amazingshop.personal.userservice.models.Chat;
import com.amazingshop.personal.userservice.models.ChatMessage;
import com.amazingshop.personal.userservice.models.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EntityMapper {

    // User mapping
    User toUser(UserDTO userDTO);
    UserDTO toUserDTO(User user);

    // Chat mapping
    ChatResponse toChatResponse(Chat chat);
    List<ChatResponse> toChatResponseList(List<Chat> chats);

    // ChatMessage mapping
    ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage);
    List<ChatMessageResponse> toChatMessageResponseList(List<ChatMessage> messages);
}
