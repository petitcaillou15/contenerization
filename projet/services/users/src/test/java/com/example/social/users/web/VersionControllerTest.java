package com.example.social.users.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VersionController.class)
class VersionControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void version_exposesServiceNameAndVersion() throws Exception {
        mvc.perform(get("/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("users"))
                .andExpect(jsonPath("$.version").value("2.0.0"));
    }
}
