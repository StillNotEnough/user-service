package com.amazingshop.personal.userservice.dto.requests;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChatTitleRequest {
    @NotBlank(message = "Title cannot be empty")
    @Size(max = 40, message = "Title must be less than 40 characters")
    private String newTitle;
}