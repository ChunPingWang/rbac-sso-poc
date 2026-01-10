package com.example.audit.infrastructure.config;

import com.example.audit.infrastructure.security.ServiceAuthInterceptor;
import com.example.audit.infrastructure.security.ServiceTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;

/**
 * Configuration for East-West (service-to-service) authentication.
 *
 * <p>Provides beans for:</p>
 * <ul>
 *   <li>ServiceTokenProvider - obtains tokens via OAuth2 Client Credentials</li>
 *   <li>ServiceAuthInterceptor - adds tokens to outgoing requests</li>
 *   <li>Pre-configured RestTemplate with authentication</li>
 * </ul>
 *
 * <p>Configuration example:</p>
 * <pre>
 * audit:
 *   security:
 *     enabled: true
 *     issuer-uri: http://localhost:8180/realms/ecommerce
 *   service-auth:
 *     enabled: true
 *     client-id: my-service-client
 *     client-secret: ${SERVICE_CLIENT_SECRET}
 * </pre>
 *
 * <p>Usage:</p>
 * <pre>
 * &#64;Autowired
 * private RestTemplate serviceRestTemplate;
 *
 * // Requests automatically include Bearer token
 * ResponseEntity&lt;?&gt; response = serviceRestTemplate.getForEntity(url, SomeType.class);
 * </pre>
 */
@Configuration
@ConditionalOnProperty(name = "audit.service-auth.enabled", havingValue = "true")
public class ServiceAuthConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ServiceAuthConfiguration.class);

    @Value("${audit.service-auth.client-id:}")
    private String clientId;

    @Value("${audit.service-auth.client-secret:}")
    private String clientSecret;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    /**
     * RestTemplate for obtaining tokens (no auth interceptor to avoid recursion).
     */
    @Bean
    @ConditionalOnMissingBean(name = "tokenRestTemplate")
    public RestTemplate tokenRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Service token provider for obtaining service tokens.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "audit.security.issuer-uri")
    public ServiceTokenProvider serviceTokenProvider(
            SecurityProperties properties,
            RestTemplate tokenRestTemplate) {

        if (clientId == null || clientId.isBlank()) {
            log.warn("Service auth enabled but client-id not configured");
            return null;
        }

        log.info("Configuring service token provider for client: {}", clientId);
        return new ServiceTokenProvider(properties, tokenRestTemplate, clientId, clientSecret);
    }

    /**
     * Interceptor for adding service tokens to outgoing requests.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ServiceTokenProvider.class)
    public ServiceAuthInterceptor serviceAuthInterceptor(ServiceTokenProvider tokenProvider) {
        log.info("Configuring service auth interceptor for: {}", serviceName);
        return new ServiceAuthInterceptor(tokenProvider, serviceName);
    }

    /**
     * Pre-configured RestTemplate with service authentication.
     */
    @Bean
    @ConditionalOnMissingBean(name = "serviceRestTemplate")
    @ConditionalOnBean(ServiceAuthInterceptor.class)
    public RestTemplate serviceRestTemplate(
            RestTemplateBuilder builder,
            ServiceAuthInterceptor interceptor) {

        log.info("Creating service RestTemplate with authentication interceptor");
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .interceptors(Collections.singletonList(interceptor))
                .build();
    }
}
