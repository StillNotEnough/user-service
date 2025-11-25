package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.dto.requests.UserDTO;
import com.amazingshop.personal.userservice.interfaces.ConverterService;
import com.amazingshop.personal.userservice.models.User;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConverterServiceImpl implements ConverterService {

    private final ModelMapper modelMapper;

    @Autowired
    public ConverterServiceImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public User convertedToUser(UserDTO userDTO) {
        return modelMapper.map(userDTO, User.class);
    }

    public UserDTO convertedToUserDTO(User user) {
        return modelMapper.map(user, UserDTO.class);
    }
}