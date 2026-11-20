package com.example.social.users;

import com.example.social.users.domain.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Le contexte Spring démarre, /health répond 200, et le seed Liquibase est chargé. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsersApplicationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository users;

    @Test
    void contextLoads_andHealthIsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void liquibaseSeedLoaded_withUsableBcryptHash() {
        var alice = users.findByEmail("alice@example.com").orElseThrow();
        Assertions.assertEquals("alice", alice.getPseudo());
        Assertions.assertTrue(
            new BCryptPasswordEncoder().matches("password", alice.getPasswordHash()));
    }
}
