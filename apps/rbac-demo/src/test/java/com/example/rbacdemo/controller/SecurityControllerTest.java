package com.example.rbacdemo.controller;

import com.example.rbacdemo.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class SecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ==================== Admin Endpoint Tests ====================

    @Test
    void adminDashboard_withoutAuth_shouldReturn403() throws Exception {
        // Without OAuth2 Resource Server in test mode, unauthenticated access returns 403
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDashboard_withAdminRole_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void adminDashboard_withManagerRole_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminDashboard_withUserRole_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void adminDashboard_withViewerRole_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
            .andExpect(status().isForbidden());
    }

    // ==================== Manager Endpoint Tests ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    void managerTeam_withAdminRole_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/manager/team"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerTeam_withManagerRole_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/manager/team"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void managerTeam_withUserRole_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/manager/team"))
            .andExpect(status().isForbidden());
    }

    // ==================== User Endpoint Tests ====================

    @Test
    @WithMockUser(roles = "USER")
    void userMe_withUserRole_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/user/me"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void userMe_withViewerRole_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/user/me"))
            .andExpect(status().isOk());
    }

    // ==================== Resource Endpoint Tests ====================

    @Test
    @WithMockUser(roles = "VIEWER")
    void listResources_withViewerRole_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/resources"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createResource_withUserRole_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/resources")
                .contentType("application/json")
                .content("{\"name\": \"Test Resource\", \"classification\": \"PUBLIC\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void createResource_withViewerRole_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/resources")
                .contentType("application/json")
                .content("{\"name\": \"Test Resource\", \"classification\": \"PUBLIC\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteResource_withAdminRole_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/resources/1"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void deleteResource_withManagerRole_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/resources/1"))
            .andExpect(status().isForbidden());
    }
}
