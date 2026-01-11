package com.example.audit.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

/**
 * mTLS WebClient 配置類 (使用 Spring Boot 3.1+ SSL Bundle)
 *
 * <p>此配置類使用 Spring Boot 3.1 引入的 SSL Bundle 功能，
 * 提供更簡潔的 mTLS WebClient 配置方式。</p>
 *
 * <h3>配置範例 (application-mtls.yml)</h3>
 * <pre>
 * spring:
 *   ssl:
 *     bundle:
 *       pem:
 *         mtls-bundle:
 *           keystore:
 *             certificate: /etc/ssl/certs/tls.crt
 *             private-key: /etc/ssl/certs/tls.key
 *           truststore:
 *             certificate: /etc/ssl/certs/ca.crt
 *
 * mtls:
 *   enabled: true
 *   bundle-name: mtls-bundle
 * </pre>
 *
 * <h3>使用方式</h3>
 * <pre>
 * &#64;Autowired
 * &#64;Qualifier("mtlsWebClient")
 * private WebClient mtlsWebClient;
 *
 * // 使用 mTLS 呼叫其他服務
 * String response = mtlsWebClient.get()
 *     .uri("https://product-service:8081/api/products")
 *     .retrieve()
 *     .bodyToMono(String.class)
 *     .block();
 * </pre>
 *
 * @author RBAC-SSO-POC Team
 * @since 1.0.0
 * @see org.springframework.boot.ssl.SslBundles
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
@ConditionalOnProperty(name = "mtls.enabled", havingValue = "true")
public class MtlsWebClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MtlsWebClientConfiguration.class);

    private static final String DEFAULT_BUNDLE_NAME = "mtls-bundle";

    /**
     * 建立配置了 mTLS 的 WebClient (使用 SSL Bundle)
     *
     * @param sslBundles Spring Boot SSL Bundles
     * @return 配置了 mTLS 的 WebClient
     */
    @Bean("mtlsWebClient")
    @Primary
    public WebClient mtlsWebClient(SslBundles sslBundles) {
        try {
            log.info("Initializing mTLS WebClient using SSL Bundle: {}", DEFAULT_BUNDLE_NAME);

            SslBundle sslBundle = sslBundles.getBundle(DEFAULT_BUNDLE_NAME);

            HttpClient httpClient = HttpClient.create()
                .secure(sslContextSpec -> {
                    try {
                        sslContextSpec.sslContext(
                            io.netty.handler.ssl.SslContextBuilder.forClient()
                                .keyManager(sslBundle.getManagers().getKeyManagerFactory())
                                .trustManager(sslBundle.getManagers().getTrustManagerFactory())
                                .build()
                        );
                    } catch (SSLException e) {
                        throw new RuntimeException("Failed to configure SSL context", e);
                    }
                });

            log.info("mTLS WebClient initialized successfully");

            return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        } catch (Exception e) {
            log.error("Failed to initialize mTLS WebClient", e);
            throw new RuntimeException("Failed to initialize mTLS WebClient", e);
        }
    }

    /**
     * 建立 WebClient.Builder 用於自訂配置
     *
     * @param sslBundles Spring Boot SSL Bundles
     * @return 配置了 mTLS 的 WebClient.Builder
     */
    @Bean("mtlsWebClientBuilder")
    public WebClient.Builder mtlsWebClientBuilder(SslBundles sslBundles) {
        try {
            SslBundle sslBundle = sslBundles.getBundle(DEFAULT_BUNDLE_NAME);

            HttpClient httpClient = HttpClient.create()
                .secure(sslContextSpec -> {
                    try {
                        sslContextSpec.sslContext(
                            io.netty.handler.ssl.SslContextBuilder.forClient()
                                .keyManager(sslBundle.getManagers().getKeyManagerFactory())
                                .trustManager(sslBundle.getManagers().getTrustManagerFactory())
                                .build()
                        );
                    } catch (SSLException e) {
                        throw new RuntimeException("Failed to configure SSL context", e);
                    }
                });

            return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        } catch (Exception e) {
            log.error("Failed to create mTLS WebClient.Builder", e);
            throw new RuntimeException("Failed to create mTLS WebClient.Builder", e);
        }
    }
}
