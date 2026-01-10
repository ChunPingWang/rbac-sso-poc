package com.example.ecommerce.user.adapter.inbound.rest;

import com.example.ecommerce.user.application.dto.UserProfileView;
import com.example.ecommerce.user.application.service.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileService userProfileService;

    @Nested
    @DisplayName("GET /api/users/me")
    class GetCurrentUser {

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "testuser", roles = {"USER"})
        @DisplayName("should return user profile when authenticated")
        void shouldReturnUserProfileWhenAuthenticated() throws Exception {
            UserProfileView profile = new UserProfileView(
                "testuser",
                "testuser@example.com",
                "Test",
                "User",
                "tenant-1",
                List.of("ROLE_USER"),
                List.of("users")
            );

            when(userProfileService.getCurrentUserProfile()).thenReturn(profile);

            mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("testuser@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("Test"))
                .andExpect(jsonPath("$.data.lastName").value("User"))
                .andExpect(jsonPath("$.data.tenantId").value("tenant-1"))
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.data.groups[0]").value("users"));
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN", "USER"})
        @DisplayName("should return admin profile with multiple roles")
        void shouldReturnAdminProfileWithMultipleRoles() throws Exception {
            UserProfileView profile = new UserProfileView(
                "admin",
                "admin@example.com",
                "Admin",
                "User",
                "system",
                List.of("ROLE_ADMIN", "ROLE_USER"),
                List.of("admins", "users")
            );

            when(userProfileService.getCurrentUserProfile()).thenReturn(profile);

            mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.tenantId").value("system"))
                .andExpect(jsonPath("$.data.roles.length()").value(2))
                .andExpect(jsonPath("$.data.groups.length()").value(2));
        }

    }
}
