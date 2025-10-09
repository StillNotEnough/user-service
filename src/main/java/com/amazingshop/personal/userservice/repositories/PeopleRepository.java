package com.amazingshop.personal.userservice.repositories;

import com.amazingshop.personal.userservice.models.Person;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PeopleRepository extends JpaRepository<Person, Long> {
    Optional<Person> findPersonByUsername(@NotEmpty(message = "Username should be not empty") @Size(min = 2, max = 30,
                                        message = "Username should be for 2 to 30 symbols") String username);

    Optional<Person> findPersonByEmail(@NotEmpty(message = "Email should be not empty") @Email(
                                               message = "Email should be valid") String email);

    Optional<Person> findByOauthProviderAndOauthId(String provider, String oauthId);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}