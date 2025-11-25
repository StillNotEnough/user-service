package com.amazingshop.personal.userservice.util.validators;

import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.services.UserService;
import com.amazingshop.personal.userservice.util.exceptions.UserValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class UserValidator implements Validator {

    private final UserService userService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @Autowired
    public UserValidator(UserService userService) {
        this.userService =  userService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return User.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        User user = (User) target;

        try {
            validateAndThrow(user);
        }catch (UserValidationException e){
            errors.rejectValue("username", "", e.getMessage());
        }
    }

    public void validateAndThrow(User user) {
        validateUsername(user.getUsername());
        validateEmail(user.getEmail());
        validatePassword(user.getPassword());
    }

    public void validateUsername(String username){
        if (username == null || username.trim().isEmpty()) {
            throw new UserValidationException("Username cannot be empty!");
        }

        if (username.length() < 2 || username.length() > 30){
            throw new UserValidationException("Username should be between 2 and 30 characters!");
        }

        Optional<User> existingUser = userService.findByUsername(username);
        if (existingUser.isPresent()){
            throw new UserValidationException("A user with this username already exists!");
        }
    }

    public void validateEmail(String email){
        if (email == null || email.trim().isEmpty()) {
            throw new UserValidationException("Email cannot be empty!");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new UserValidationException("Invalid email format!");
        }

        Optional<User> existingUser = userService.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new UserValidationException("A user with this email already exists!");
        }
    }

    public void validatePassword(String password){
        if (password == null || password.trim().isEmpty()) {
            throw new UserValidationException("Password cannot be empty!");
        }

        if (password.length() < 6) {
            throw new UserValidationException("Password should be at least 6 characters long!");
        }
    }
}