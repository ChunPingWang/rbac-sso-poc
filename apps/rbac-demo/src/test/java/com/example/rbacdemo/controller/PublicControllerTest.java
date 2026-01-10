package com.example.rbacdemo.controller;

import com.example.rbacdemo.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class PublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getPublicInfo_shouldReturnApplicationInfo() throws Exception {
        mockMvc.perform(get("/api/public/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.application").value("RBAC SSO POC"))
            .andExpect(jsonPath("$.version").value("1.0.0-SNAPSHOT"));
    }

    @Test
    void healthCheck_shouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/public/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
}
