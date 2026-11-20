package com.example.social.posts;

import com.example.social.posts.users.UsersClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Le contexte démarre ; /health répond 200 (users mické en UP, DB = H2). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostsApplicationTests {

    @Autowired MockMvc mockMvc;

    // Empêche tout appel réseau réel vers users pendant le test de contexte.
    @MockitoBean UsersClient usersClient;

    @Test
    void contextLoads_andHealthIsUp() throws Exception {
        when(usersClient.ping()).thenReturn(true);
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
