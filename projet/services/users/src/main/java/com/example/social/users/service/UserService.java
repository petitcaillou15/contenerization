package com.example.social.users.service;

import com.example.social.users.domain.User;
import com.example.social.users.domain.UserRepository;
import com.example.social.users.service.exception.DuplicateUserException;
import com.example.social.users.service.exception.InvalidCredentialsException;
import com.example.social.users.service.exception.UserNotFoundException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Règles métier du service users. */
@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Counter registeredCounter;

    public UserService(UserRepository users, PasswordEncoder encoder, MeterRegistry registry) {
        this.users = users;
        this.encoder = encoder;
        // Métrique métier exigée par la spec §6.
        this.registeredCounter = Counter.builder("users_registered_total")
                .description("Nombre total d'inscriptions")
                .register(registry);
    }

    @Transactional
    public User register(String pseudo, String email, String rawPassword) {
        if (users.existsByPseudoOrEmail(pseudo, email)) {
            throw new DuplicateUserException();
        }
        User saved = users.save(new User(pseudo, email, encoder.encode(rawPassword)));
        registeredCounter.increment();
        return saved;
    }

    @Transactional(readOnly = true)
    public User authenticate(String email, String rawPassword) {
        User user = users.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (!encoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User getById(long id) {
        return users.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public User updateBio(long id, String bio) {
        User user = getById(id);
        user.setBio(bio);
        return user; // @Transactional -> flush automatique
    }
}
