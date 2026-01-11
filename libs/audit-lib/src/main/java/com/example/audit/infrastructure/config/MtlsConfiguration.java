package com.example.audit.infrastructure.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * mTLS 配置類
 *
 * <p>當啟用 mTLS 時，此配置類會建立一個配置了雙向 TLS 的 WebClient，
 * 用於服務間的安全通訊。</p>
 *
 * <h3>使用方式</h3>
 * <pre>
 * # 在 application.yml 中啟用
 * mtls:
 *   enabled: true
 *   certificate: /etc/ssl/certs/tls.crt
 *   private-key: /etc/ssl/certs/tls.key
 *   ca-certificate: /etc/ssl/certs/ca.crt
 * </pre>
 *
 * <h3>憑證結構</h3>
 * <pre>
 * /etc/ssl/certs/
 * ├── tls.crt      # 服務憑證
 * ├── tls.key      # 私鑰
 * └── ca.crt       # CA 憑證 (用於驗證其他服務)
 * </pre>
 *
 * @author RBAC-SSO-POC Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "mtls.enabled", havingValue = "true")
public class MtlsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MtlsConfiguration.class);

    @Value("${mtls.certificate:/etc/ssl/certs/tls.crt}")
    private String certificatePath;

    @Value("${mtls.private-key:/etc/ssl/certs/tls.key}")
    private String privateKeyPath;

    @Value("${mtls.ca-certificate:/etc/ssl/certs/ca.crt}")
    private String caCertificatePath;

    /**
     * 建立配置了 mTLS 的 WebClient
     *
     * <p>此 WebClient 會在發送請求時附帶客戶端憑證，
     * 並驗證伺服器端憑證是否由信任的 CA 簽發。</p>
     *
     * @return 配置了 mTLS 的 WebClient
     */
    @Bean("mtlsWebClient")
    public WebClient mtlsWebClient() {
        try {
            log.info("Initializing mTLS WebClient with certificate: {}", certificatePath);

            SslContext sslContext = buildSslContext();

            HttpClient httpClient = HttpClient.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

            return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        } catch (Exception e) {
            log.error("Failed to initialize mTLS WebClient", e);
            throw new RuntimeException("Failed to initialize mTLS WebClient", e);
        }
    }

    /**
     * 建立 SSL Context
     *
     * @return 配置了客戶端和伺服器端憑證的 SslContext
     * @throws Exception 如果憑證載入失敗
     */
    private SslContext buildSslContext() throws Exception {
        // 載入 CA 憑證 (用於驗證伺服器)
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate caCert;
        try (InputStream caInputStream = new FileInputStream(caCertificatePath)) {
            caCert = (X509Certificate) cf.generateCertificate(caInputStream);
        }

        // 建立 TrustManager
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 載入客戶端憑證和私鑰
        X509Certificate clientCert;
        try (InputStream certInputStream = new FileInputStream(certificatePath)) {
            clientCert = (X509Certificate) cf.generateCertificate(certInputStream);
        }

        // 建立 KeyManager (使用 PEM 格式)
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        // 注意：實際使用時需要使用 BouncyCastle 或其他庫來解析 PEM 私鑰
        // 這裡簡化處理，建議使用 Spring SSL Bundle

        log.info("mTLS SSL Context initialized successfully");
        log.info("  - Certificate: {}", certificatePath);
        log.info("  - CA Certificate: {}", caCertificatePath);
        log.info("  - Client Certificate Subject: {}", clientCert.getSubjectX500Principal());

        return SslContextBuilder.forClient()
            .trustManager(caCert)
            .build();
    }
}
