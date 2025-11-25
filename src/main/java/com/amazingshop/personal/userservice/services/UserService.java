package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.models.User;
import com.amazingshop.personal.userservice.repositories.UsersRepository;
import com.amazingshop.personal.userservice.util.exceptions.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UsersRepository usersRepository;

    @Autowired
    public UserService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public Optional<User> findByUsername(String username) {
        log.debug("Searching for user by username: {}", username);
        return usersRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        log.debug("Searching for user by email: {}", email);
        return usersRepository.findByEmail(email);
    }

    public User findUserByIdOrThrow(Long id) {
        return usersRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));
    }

    public List<User> findAll() {
        return usersRepository.findAll();
    }

    @Transactional
    public User save(User user) {
        log.debug("Saving user: {}", user.getUsername());
        return usersRepository.save(user);
    }

    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting user with id: {}", id);
        if (!usersRepository.existsById(id)) {
            throw new UserNotFoundException("User with id " + id + " not found");
        }
        usersRepository.deleteById(id);
    }
}