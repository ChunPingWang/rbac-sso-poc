package com.example.audit.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Security auto-configuration for audit-lib.
 *
 * <p>Provides:</p>
 * <ul>
 *   <li>OAuth2 Resource Server with JWT validation (North-South security)</li>
 *   <li>CORS configuration</li>
 *   <li>Method-level security (@PreAuthorize)</li>
 *   <li>Role extraction from Keycloak JWT tokens</li>
 * </ul>
 *
 * <p>Enable by setting:</p>
 * <pre>
 * audit:
 *   security:
 *     enabled: true
 *     issuer-uri: http://localhost:8180/realms/ecommerce
 * </pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnClass({SecurityFilterChain.class, JwtDecoder.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "audit.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    /**
     * Main security configuration with OAuth2 Resource Server.
     */
    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
    @ConditionalOnProperty(name = "audit.security.issuer-uri")
    static class OAuth2SecurityConfig {

        private final SecurityProperties properties;

        OAuth2SecurityConfig(SecurityProperties properties) {
            this.properties = properties;
        }

        @Bean
        @ConditionalOnMissingBean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            log.info("Configuring OAuth2 Resource Server security with issuer: {}",
                    properties.getIssuerUri());

            // Build public paths array
            String[] publicPaths = properties.getPublicPaths().toArray(new String[0]);

            http
                // Disable CSRF for stateless API
                .csrf(AbstractHttpConfigurer::disable)

                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless session management
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                    // Public endpoints
                    .requestMatchers(publicPaths).permitAll()
                    // Audit endpoints require authentication
                    .requestMatchers("/api/v1/audit-logs/**").authenticated()
                    // All other requests require authentication
                    .anyRequest().authenticated()
                )

                // OAuth2 Resource Server configuration
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    )
                );

            return http.build();
        }

        @Bean
        @ConditionalOnMissingBean
        public JwtDecoder jwtDecoder() {
            if (properties.getJwkSetUri() != null && !properties.getJwkSetUri().isBlank()) {
                log.info("Using JWK Set URI: {}", properties.getJwkSetUri());
                return NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
            }
            log.info("Using issuer URI for JWT decoding: {}", properties.getIssuerUri());
            return JwtDecoders.fromIssuerLocation(properties.getIssuerUri());
        }

        @Bean
        @ConditionalOnMissingBean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
            JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
            return converter;
        }

        @Bean
        @ConditionalOnMissingBean
        public CorsConfigurationSource corsConfigurationSource() {
            SecurityProperties.CorsProperties corsProps = properties.getCors();

            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(corsProps.getAllowedOrigins());
            configuration.setAllowedMethods(corsProps.getAllowedMethods());
            configuration.setAllowedHeaders(corsProps.getAllowedHeaders());
            configuration.setExposedHeaders(corsProps.getExposedHeaders());
            configuration.setAllowCredentials(corsProps.isAllowCredentials());
            configuration.setMaxAge(corsProps.getMaxAge());

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);

            log.info("CORS configured with allowed origins: {}", corsProps.getAllowedOrigins());
            return source;
        }
    }

    /**
     * Fallback security configuration when issuer-uri is not set.
     * Permits all requests but logs a warning.
     */
    @Configuration
    @EnableWebSecurity
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    static class FallbackSecurityConfig {

        private static final Logger log = LoggerFactory.getLogger(FallbackSecurityConfig.class);

        private final SecurityProperties properties;

        FallbackSecurityConfig(SecurityProperties properties) {
            this.properties = properties;
        }

        @Bean
        public SecurityFilterChain fallbackSecurityFilterChain(HttpSecurity http) throws Exception {
            log.warn("=================================================================");
            log.warn("SECURITY WARNING: No issuer-uri configured!");
            log.warn("OAuth2 JWT validation is DISABLED.");
            log.warn("Set 'audit.security.issuer-uri' for production use.");
            log.warn("=================================================================");

            http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
                );

            return http.build();
        }

        @Bean
        @ConditionalOnMissingBean
        public CorsConfigurationSource corsConfigurationSource() {
            SecurityProperties.CorsProperties corsProps = properties.getCors();

            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(corsProps.getAllowedOrigins());
            configuration.setAllowedMethods(corsProps.getAllowedMethods());
            configuration.setAllowedHeaders(corsProps.getAllowedHeaders());
            configuration.setExposedHeaders(corsProps.getExposedHeaders());
            configuration.setAllowCredentials(corsProps.isAllowCredentials());
            configuration.setMaxAge(corsProps.getMaxAge());

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }
    }

    /**
     * Converts Keycloak realm roles from JWT to Spring Security GrantedAuthority.
     *
     * <p>Keycloak JWT structure:</p>
     * <pre>
     * {
     *   "realm_access": {
     *     "roles": ["ADMIN", "USER"]
     *   },
     *   "resource_access": {
     *     "account": {
     *       "roles": ["manage-account"]
     *     }
     *   }
     * }
     * </pre>
     */
    static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        private static final String REALM_ACCESS_CLAIM = "realm_access";
        private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
        private static final String ROLES_CLAIM = "roles";

        @Override
        @SuppressWarnings("unchecked")
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Set<GrantedAuthority> authorities = new HashSet<>();

            // Extract realm roles
            Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
            if (realmAccess != null && realmAccess.containsKey(ROLES_CLAIM)) {
                List<String> roles = (List<String>) realmAccess.get(ROLES_CLAIM);
                roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .forEach(authorities::add);
            }

            // Extract resource/client roles (optional)
            Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS_CLAIM);
            if (resourceAccess != null) {
                resourceAccess.forEach((clientId, clientRoles) -> {
                    if (clientRoles instanceof Map) {
                        Map<String, Object> clientRolesMap = (Map<String, Object>) clientRoles;
                        if (clientRolesMap.containsKey(ROLES_CLAIM)) {
                            List<String> roles = (List<String>) clientRolesMap.get(ROLES_CLAIM);
                            roles.stream()
                                .map(role -> new SimpleGrantedAuthority(
                                    "ROLE_" + clientId.toUpperCase() + "_" + role.toUpperCase()))
                                .forEach(authorities::add);
                        }
                    }
                });
            }

            // Add scope-based authorities
            String scope = jwt.getClaimAsString("scope");
            if (scope != null && !scope.isBlank()) {
                Arrays.stream(scope.split(" "))
                    .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                    .forEach(authorities::add);
            }

            return authorities;
        }
    }
}
