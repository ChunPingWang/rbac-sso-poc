package com.example.rbacdemo.service;

import com.example.rbacdemo.dto.UserInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CurrentUserServiceTest {

    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserService();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_withNoAuthentication_shouldReturnAnonymous() {
        UserInfo user = currentUserService.getCurrentUser();

        assertEquals("anonymous", user.getUsername());
        assertFalse(user.isAuthenticated());
        assertTrue(user.getRoles().isEmpty());
    }

    @Test
    void getCurrentUser_withJwtToken_shouldExtractUserInfo() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("sub", "user-123")
            .claim("preferred_username", "testuser")
            .claim("email", "test@example.com")
            .claim("given_name", "Test")
            .claim("family_name", "User")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_VIEWER")
        );

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Act
        UserInfo user = currentUserService.getCurrentUser();

        // Assert
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test", user.getFirstName());
        assertEquals("User", user.getLastName());
        assertEquals("user-123", user.getSubject());
        assertTrue(user.isAuthenticated());
        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().contains("ROLE_USER"));
        assertTrue(user.getRoles().contains("ROLE_VIEWER"));
    }

    @Test
    void hasRole_withMatchingRole_shouldReturnTrue() {
        // Arrange
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("sub", "user-123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Act & Assert
        assertTrue(currentUserService.hasRole("ADMIN"));
        assertTrue(currentUserService.hasRole("ROLE_ADMIN"));
        assertTrue(currentUserService.isAdmin());
        assertFalse(currentUserService.isManager());
        assertFalse(currentUserService.isUser());
    }

    @Test
    void getFullName_shouldConcatenateFirstAndLastName() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claim("sub", "user-123")
            .claim("preferred_username", "testuser")
            .claim("given_name", "John")
            .claim("family_name", "Doe")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        UserInfo user = currentUserService.getCurrentUser();

        assertEquals("John Doe", user.getFullName());
    }
}
