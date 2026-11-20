package com.example.social.posts.health;

import com.example.social.posts.users.UsersClient;
import com.example.social.posts.users.UsersUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** users DOWN : GET /posts OK, /health = 200 body DEGRADED, POST /posts = 503. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DegradedHealthWebTest {

    @Autowired MockMvc mvc;
    @MockitoBean UsersClient usersClient;

    @Test
    void usersDown_degradesHealthButKeepsReadsAndBlocksWrites() throws Exception {
        when(usersClient.ping()).thenReturn(false);
        when(usersClient.verify(any())).thenThrow(new UsersUnavailableException("down"));

        // lecture publique : OK
        mvc.perform(get("/posts")).andExpect(status().isOk());

        // santé : dégradée mais HTTP 200
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"));

        // écriture : 503 avec le message de la spec
        mvc.perform(post("/posts")
                        .header("Authorization", "Bearer whatever")
                        .contentType("application/json")
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("writes temporarily disabled"));
    }
}
