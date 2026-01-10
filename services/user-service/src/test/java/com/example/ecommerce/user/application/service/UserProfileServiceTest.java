package com.example.ecommerce.user.application.service;

import com.example.ecommerce.user.application.dto.UserProfileView;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService")
class UserProfileServiceTest {

    private UserProfileService userProfileService;
    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService();
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Get Current User Profile")
    class GetCurrentUserProfile {

        @Test
        @DisplayName("should throw when not authenticated")
        void shouldThrowWhenNotAuthenticated() {
            when(securityContext.getAuthentication()).thenReturn(null);

            assertThrows(IllegalStateException.class,
                () -> userProfileService.getCurrentUserProfile());
        }

        @Test
        @DisplayName("should throw when authentication is not authenticated")
        void shouldThrowWhenAuthenticationIsNotAuthenticated() {
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(false);

            assertThrows(IllegalStateException.class,
                () -> userProfileService.getCurrentUserProfile());
        }

        @Test
        @DisplayName("should return profile with basic auth")
        void shouldReturnProfileWithBasicAuth() {
            Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("testuser");
            when(authentication.getPrincipal()).thenReturn("testuser");
            doReturn(authorities).when(authentication).getAuthorities();

            UserProfileView profile = userProfileService.getCurrentUserProfile();

            assertEquals("testuser", profile.username());
            assertNull(profile.email());
            assertNull(profile.firstName());
            assertNull(profile.lastName());
            assertEquals("default", profile.tenantId());
            assertEquals(1, profile.roles().size());
            assertTrue(profile.roles().contains("ROLE_USER"));
            assertTrue(profile.groups().isEmpty());
        }

        @Test
        @DisplayName("should extract info from JWT")
        void shouldExtractInfoFromJwt() {
            Jwt jwt = createMockJwt(
                "jwtuser",
                "jwtuser@example.com",
                "John",
                "Doe",
                "tenant-1",
                List.of("admin-group", "user-group")
            );

            Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
            );

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("jwtuser");
            when(authentication.getPrincipal()).thenReturn(jwt);
            doReturn(authorities).when(authentication).getAuthorities();

            UserProfileView profile = userProfileService.getCurrentUserProfile();

            assertEquals("jwtuser", profile.username());
            assertEquals("jwtuser@example.com", profile.email());
            assertEquals("John", profile.firstName());
            assertEquals("Doe", profile.lastName());
            assertEquals("tenant-1", profile.tenantId());
            assertEquals(2, profile.roles().size());
            assertTrue(profile.roles().contains("ROLE_ADMIN"));
            assertTrue(profile.roles().contains("ROLE_USER"));
            assertEquals(2, profile.groups().size());
            assertTrue(profile.groups().contains("admin-group"));
            assertTrue(profile.groups().contains("user-group"));
        }

        @Test
        @DisplayName("should use default tenant when not in JWT")
        void shouldUseDefaultTenantWhenNotInJwt() {
            Jwt jwt = createMockJwt(
                "jwtuser",
                "jwtuser@example.com",
                "John",
                "Doe",
                null,  // No tenant
                null   // No groups
            );

            Collection<GrantedAuthority> authorities = Collections.emptyList();

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("jwtuser");
            when(authentication.getPrincipal()).thenReturn(jwt);
            doReturn(authorities).when(authentication).getAuthorities();

            UserProfileView profile = userProfileService.getCurrentUserProfile();

            assertEquals("default", profile.tenantId());
            assertTrue(profile.groups().isEmpty());
        }

        @Test
        @DisplayName("should handle multiple roles")
        void shouldHandleMultipleRoles() {
            Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_MANAGER")
            );

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("admin");
            when(authentication.getPrincipal()).thenReturn("admin");
            doReturn(authorities).when(authentication).getAuthorities();

            UserProfileView profile = userProfileService.getCurrentUserProfile();

            assertEquals(3, profile.roles().size());
            assertTrue(profile.roles().contains("ROLE_ADMIN"));
            assertTrue(profile.roles().contains("ROLE_USER"));
            assertTrue(profile.roles().contains("ROLE_MANAGER"));
        }
    }

    private Jwt createMockJwt(String subject, String email, String givenName,
                               String familyName, String tenantId, List<String> groups) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        if (email != null) claims.put("email", email);
        if (givenName != null) claims.put("given_name", givenName);
        if (familyName != null) claims.put("family_name", familyName);
        if (tenantId != null) claims.put("tenant_id", tenantId);
        if (groups != null) claims.put("groups", groups);

        return new Jwt(
            "test-token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "RS256"),
            claims
        );
    }
}
