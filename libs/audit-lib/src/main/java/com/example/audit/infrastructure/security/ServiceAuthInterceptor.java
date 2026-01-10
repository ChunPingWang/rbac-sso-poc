package com.example.audit.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * HTTP client interceptor for East-West service authentication.
 *
 * <p>Automatically adds Bearer token to outgoing HTTP requests
 * for service-to-service communication.</p>
 *
 * <p>Usage with RestTemplate:</p>
 * <pre>
 * RestTemplate restTemplate = new RestTemplate();
 * restTemplate.getInterceptors().add(
 *     new ServiceAuthInterceptor(serviceTokenProvider)
 * );
 * </pre>
 *
 * <p>Headers added:</p>
 * <ul>
 *   <li>Authorization: Bearer {token}</li>
 *   <li>X-Service-Name: {serviceName} (optional)</li>
 * </ul>
 */
public class ServiceAuthInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ServiceAuthInterceptor.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SERVICE_NAME_HEADER = "X-Service-Name";

    private final ServiceTokenProvider tokenProvider;
    private final String serviceName;

    /**
     * Create interceptor with token provider.
     *
     * @param tokenProvider the service token provider
     */
    public ServiceAuthInterceptor(ServiceTokenProvider tokenProvider) {
        this(tokenProvider, null);
    }

    /**
     * Create interceptor with token provider and service name.
     *
     * @param tokenProvider the service token provider
     * @param serviceName   optional service name to include in headers
     */
    public ServiceAuthInterceptor(ServiceTokenProvider tokenProvider, String serviceName) {
        this.tokenProvider = tokenProvider;
        this.serviceName = serviceName;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        // Skip if Authorization header already present
        if (request.getHeaders().containsKey(AUTHORIZATION_HEADER)) {
            log.trace("Authorization header already present, skipping token injection");
            return execution.execute(request, body);
        }

        // Get token and add to request
        tokenProvider.getToken().ifPresentOrElse(
            token -> {
                request.getHeaders().add(AUTHORIZATION_HEADER, BEARER_PREFIX + token);
                log.trace("Added service token to request: {} {}",
                        request.getMethod(), request.getURI());
            },
            () -> log.warn("Failed to obtain service token for request: {} {}",
                    request.getMethod(), request.getURI())
        );

        // Add service name header if configured
        if (serviceName != null && !serviceName.isBlank()) {
            request.getHeaders().add(SERVICE_NAME_HEADER, serviceName);
        }

        return execution.execute(request, body);
    }
}
