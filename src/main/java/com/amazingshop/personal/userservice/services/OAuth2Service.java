package com.amazingshop.personal.userservice.services;

import com.amazingshop.personal.userservice.dto.responses.OAuth2UserInfo;
import com.amazingshop.personal.userservice.enums.Role;
import com.amazingshop.personal.userservice.models.Person;
import com.amazingshop.personal.userservice.repositories.PeopleRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
public class OAuth2Service {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    private final PeopleRepository peopleRepository;
    private final GoogleIdTokenVerifier googleVerifier;

    @Autowired
    public OAuth2Service(PeopleRepository peopleRepository,
                         @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId) {
        this.peopleRepository = peopleRepository;

        // Создаем верификатор для Google ID токенов
        this.googleVerifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Верификация Google ID Token и получение информации о пользователе
     */
    public OAuth2UserInfo verifyGoogleToken(String idToken) {
        try {
            GoogleIdToken token = googleVerifier.verify(idToken);

            if (token == null) {
                throw new IllegalArgumentException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = token.getPayload();

            return OAuth2UserInfo.builder()
                    .id(payload.getSubject())
                    .email(payload.getEmail())
                    .name((String) payload.get("name"))
                    .picture((String) payload.get("picture"))
                    .provider("google")
                    .build();

        } catch (Exception e) {
            log.error("Error verifying Google token: {}", e.getMessage());
            throw new RuntimeException("Failed to verify Google token", e);
        }
    }

    /**
     * Поиск или создание пользователя через OAuth2
     */
    @Transactional
    public Person findOrCreateOAuth2User(OAuth2UserInfo userInfo) {
        log.info("Finding or creating OAuth2 user: {} from {}", userInfo.getEmail(), userInfo.getProvider());

        // Ищем пользователя по OAuth ID
        Optional<Person> existingPerson = peopleRepository
                .findByOauthProviderAndOauthId(userInfo.getProvider(), userInfo.getId());

        if (existingPerson.isPresent()) {
            log.info("Found existing OAuth2 user: {}", existingPerson.get().getUsername());
            return existingPerson.get();
        }

        // Проверяем, существует ли пользователь с таким email (обычная регистрация)
        Optional<Person> emailUser = peopleRepository.findPersonByEmail(userInfo.getEmail());

        if (emailUser.isPresent()) {
            Person person = emailUser.get();

            // Если у пользователя уже есть обычная регистрация, привязываем OAuth
            if (person.getOauthProvider() == null) {
                person.setOauthProvider(userInfo.getProvider());
                person.setOauthId(userInfo.getId());
                person.setProfilePictureUrl(userInfo.getPicture());

                log.info("Linked OAuth2 to existing user: {}", person.getUsername());
                return peopleRepository.save(person);
            }

            throw new RuntimeException("User with this email already exists with different OAuth provider");
        }

        // Создаем нового пользователя
        Person newPerson = new Person();
        newPerson.setEmail(userInfo.getEmail());
        newPerson.setUsername(generateUsernameFromEmail(userInfo.getEmail()));
        newPerson.setOauthProvider(userInfo.getProvider());
        newPerson.setOauthId(userInfo.getId());
        newPerson.setProfilePictureUrl(userInfo.getPicture());
        newPerson.setRole(Role.USER);
        newPerson.setCreatedAt(java.time.LocalDateTime.now());
        // Password остается null для OAuth2 пользователей

        Person savedPerson = peopleRepository.save(newPerson);
        log.info("Created new OAuth2 user: {}", savedPerson.getUsername());

        return savedPerson;
    }

    /**
     * Генерация уникального username из email
     */
    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;

        while (peopleRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}
