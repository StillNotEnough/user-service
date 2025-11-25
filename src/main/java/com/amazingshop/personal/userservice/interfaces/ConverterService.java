package com.amazingshop.personal.userservice.interfaces;

import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.amazingshop.personal.userservice.models.User;

public interface ConverterService {
    User convertedToUser(UserDTO userDTO);
    UserDTO convertedToUserDTO(User user);
}
