package com.example.audit.infrastructure.security;

import com.example.audit.infrastructure.config.SecurityProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service token provider for East-West (service-to-service) authentication.
 *
 * <p>Implements OAuth2 Client Credentials flow to obtain access tokens
 * for service-to-service communication.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Token caching with automatic refresh before expiry</li>
 *   <li>Thread-safe token storage</li>
 *   <li>Configurable buffer time before token refresh</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 * String token = serviceTokenProvider.getToken()
 *     .orElseThrow(() -> new SecurityException("Failed to obtain service token"));
 *
 * restTemplate.getForObject(url, Response.class,
 *     headers -> headers.setBearerAuth(token));
 * </pre>
 */
public class ServiceTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(ServiceTokenProvider.class);

    /**
     * Buffer time in seconds before token expiry to trigger refresh.
     */
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 60;

    private final SecurityProperties properties;
    private final RestTemplate restTemplate;
    private final String tokenEndpoint;
    private final String clientId;
    private final String clientSecret;

    /**
     * Cached tokens by service name.
     */
    private final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    public ServiceTokenProvider(
            SecurityProperties properties,
            RestTemplate restTemplate,
            String clientId,
            String clientSecret) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenEndpoint = buildTokenEndpoint(properties.getIssuerUri());
    }

    /**
     * Get a valid access token, refreshing if necessary.
     *
     * @return access token if available
     */
    public Optional<String> getToken() {
        return getToken(clientId);
    }

    /**
     * Get a valid access token for a specific service account.
     *
     * @param serviceClientId the service client ID
     * @return access token if available
     */
    public Optional<String> getToken(String serviceClientId) {
        CachedToken cached = tokenCache.get(serviceClientId);

        if (cached != null && !cached.isExpiringSoon()) {
            return Optional.of(cached.accessToken);
        }

        synchronized (this) {
            // Double-check after acquiring lock
            cached = tokenCache.get(serviceClientId);
            if (cached != null && !cached.isExpiringSoon()) {
                return Optional.of(cached.accessToken);
            }

            // Fetch new token
            return fetchNewToken(serviceClientId);
        }
    }

    /**
     * Fetch a new token from the authorization server.
     */
    private Optional<String> fetchNewToken(String serviceClientId) {
        try {
            log.debug("Fetching new token for service: {}", serviceClientId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(serviceClientId, clientSecret);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("scope", "audit:read audit:write");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                    tokenEndpoint, request, TokenResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                TokenResponse tokenResponse = response.getBody();
                CachedToken cachedToken = new CachedToken(
                        tokenResponse.accessToken,
                        Instant.now().plusSeconds(tokenResponse.expiresIn)
                );
                tokenCache.put(serviceClientId, cachedToken);

                log.info("Successfully obtained token for service: {}, expires in: {}s",
                        serviceClientId, tokenResponse.expiresIn);
                return Optional.of(tokenResponse.accessToken);
            }

            log.error("Failed to obtain token for service: {}, status: {}",
                    serviceClientId, response.getStatusCode());
            return Optional.empty();

        } catch (RestClientException e) {
            log.error("Error fetching token for service: {}", serviceClientId, e);
            return Optional.empty();
        }
    }

    /**
     * Clear all cached tokens.
     */
    public void clearCache() {
        tokenCache.clear();
        log.info("Token cache cleared");
    }

    /**
     * Build token endpoint URL from issuer URI.
     */
    private String buildTokenEndpoint(String issuerUri) {
        if (issuerUri == null || issuerUri.isBlank()) {
            throw new IllegalArgumentException("Issuer URI is required for service authentication");
        }
        // Keycloak format: {issuer}/protocol/openid-connect/token
        String endpoint = issuerUri.endsWith("/")
                ? issuerUri + "protocol/openid-connect/token"
                : issuerUri + "/protocol/openid-connect/token";
        log.debug("Token endpoint: {}", endpoint);
        return endpoint;
    }

    /**
     * Cached token with expiry tracking.
     */
    private static class CachedToken {
        final String accessToken;
        final Instant expiresAt;

        CachedToken(String accessToken, Instant expiresAt) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
        }

        boolean isExpiringSoon() {
            return Instant.now().plusSeconds(TOKEN_REFRESH_BUFFER_SECONDS).isAfter(expiresAt);
        }
    }

    /**
     * Token response from authorization server.
     */
    private static class TokenResponse {
        @JsonProperty("access_token")
        String accessToken;

        @JsonProperty("expires_in")
        long expiresIn;

        @JsonProperty("token_type")
        String tokenType;

        @JsonProperty("scope")
        String scope;
    }
}
