package com.example.social.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie que /metrics expose la métrique métier et les métriques HTTP.
 * {@code @AutoConfigureObservability} est nécessaire : Spring Boot désactive
 * les exporters de métriques dans les tests par défaut.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
class ObservabilityTest {

    @Autowired MockMvc mvc;

    @Test
    void metricsEndpoint_exposesBusinessAndHttpMetrics() throws Exception {
        // provoque au moins une inscription pour matérialiser le compteur
        mvc.perform(post("/register").contentType("application/json")
                .content("{\"pseudo\":\"obs\",\"email\":\"obs@example.com\",\"password\":\"s3cret1\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("users_registered_total")))
                .andExpect(content().string(containsString("http_server_requests_seconds")));
    }
}
