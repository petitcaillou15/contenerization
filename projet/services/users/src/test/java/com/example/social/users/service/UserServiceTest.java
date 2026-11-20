package com.example.social.users.service;

import com.example.social.users.domain.User;
import com.example.social.users.domain.UserRepository;
import com.example.social.users.service.exception.DuplicateUserException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository repo;
    private PasswordEncoder encoder;
    private SimpleMeterRegistry registry;
    private UserService service;

    @BeforeEach
    void setUp() {
        repo = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        registry = new SimpleMeterRegistry();
        service = new UserService(repo, encoder, registry);
    }

    @Test
    void register_hashesPassword_savesUser_incrementsCounter() {
        when(repo.existsByPseudoOrEmail("alice", "alice@example.com")).thenReturn(false);
        when(encoder.encode("s3cret")).thenReturn("HASHED");
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.register("alice", "alice@example.com", "s3cret");

        assertEquals(1.0, registry.counter("users_registered_total").count());
    }

    @Test
    void register_rejectsDuplicate() {
        when(repo.existsByPseudoOrEmail(anyString(), anyString())).thenReturn(true);
        assertThrows(DuplicateUserException.class,
                () -> service.register("alice", "alice@example.com", "s3cret"));
    }
}
