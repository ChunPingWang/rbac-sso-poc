package com.example.ecommerce.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@WebFluxTest
@Import(GatewaySecurityConfig.class)
@DisplayName("GatewaySecurityConfig")
class GatewaySecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Nested
    @DisplayName("Public Endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("should allow access to actuator without authentication")
        void shouldAllowAccessToActuatorWithoutAuthentication() {
            webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().value(status ->
                    assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status,
                        "Expected non-401 status for actuator endpoint"));
        }

        @Test
        @DisplayName("should allow access to public API without authentication")
        void shouldAllowAccessToPublicApiWithoutAuthentication() {
            webTestClient.get()
                .uri("/api/public/health")
                .exchange()
                .expectStatus().value(status ->
                    assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status,
                        "Expected non-401 status for public API endpoint"));
        }
    }

    @Nested
    @DisplayName("Protected Endpoints")
    class ProtectedEndpoints {

        @Test
        @DisplayName("should require authentication for API endpoints")
        void shouldRequireAuthenticationForApiEndpoints() {
            webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("should require authentication for any other endpoints")
        void shouldRequireAuthenticationForAnyOtherEndpoints() {
            webTestClient.get()
                .uri("/some/other/endpoint")
                .exchange()
                .expectStatus().isUnauthorized();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should allow authenticated user to access API")
        void shouldAllowAuthenticatedUserToAccessApi() {
            webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().value(status ->
                    assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status,
                        "Expected authenticated access but got 401"));
        }
    }
}
