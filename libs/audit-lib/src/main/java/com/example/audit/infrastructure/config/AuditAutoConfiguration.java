package com.example.audit.infrastructure.config;

import com.example.audit.application.service.AuditQueryService;
import com.example.audit.application.event.AuditEventBuilder;
import com.example.audit.domain.port.AuditEventPublisher;
import com.example.audit.domain.port.AuditLogRepository;
import com.example.audit.infrastructure.aspect.AuditAspect;
import com.example.audit.infrastructure.context.AuditContextHolder;
import com.example.audit.infrastructure.event.AuditDomainEventListener;
import com.example.audit.infrastructure.event.SpringAuditEventPublisher;
import com.example.audit.infrastructure.health.AuditHealthIndicator;
import com.example.audit.infrastructure.metrics.AuditMetrics;
import com.example.audit.infrastructure.persistence.JpaAuditLogRepository;
import com.example.audit.infrastructure.persistence.SpringDataAuditLogRepository;
import com.example.audit.infrastructure.persistence.mapper.AuditLogMapper;
import com.example.audit.infrastructure.processor.FieldMasker;
import com.example.audit.infrastructure.processor.PayloadProcessor;
import com.example.audit.infrastructure.processor.maskers.CreditCardFieldMasker;
import com.example.audit.infrastructure.processor.maskers.EmailFieldMasker;
import com.example.audit.infrastructure.processor.maskers.PasswordFieldMasker;
import com.example.audit.infrastructure.web.AuditQueryController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;

/**
 * Auto-configuration for the audit library.
 *
 * <p>Enables audit logging when the property audit.enabled=true (default).</p>
 *
 * <p>Usage:</p>
 * <pre>
 * # application.yml
 * audit:
 *   enabled: true
 *   service-name: my-service
 * </pre>
 *
 * <p>Dynamic configuration reload is supported via Spring Cloud Config or
 * Spring Boot Actuator's /actuator/refresh endpoint. When AuditProperties
 * is annotated with @RefreshScope, configuration changes will be picked up
 * at runtime without restart.</p>
 *
 * <p>This configuration supports two audit mechanisms:</p>
 * <ul>
 *   <li><b>AOP-based (legacy):</b> Use @Auditable annotation on methods</li>
 *   <li><b>Domain Event-based (recommended):</b> Publish AuditableDomainEvent through AuditEventPublisher</li>
 * </ul>
 */
@AutoConfiguration
@EnableAspectJAutoProxy
@EnableAsync
@ConfigurationPropertiesScan(basePackages = "com.example.audit.infrastructure.config")
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
@EnableJpaRepositories(basePackages = "com.example.audit.infrastructure.persistence")
@EntityScan(basePackages = "com.example.audit.infrastructure.persistence.entity")
public class AuditAutoConfiguration {

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper auditObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditLogMapper auditLogMapper() {
        return new AuditLogMapper();
    }

    @Bean
    @ConditionalOnMissingBean(AuditLogRepository.class)
    public AuditLogRepository auditLogRepository(
            SpringDataAuditLogRepository springDataRepository,
            AuditLogMapper mapper) {
        return new JpaAuditLogRepository(springDataRepository, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditContextHolder auditContextHolder() {
        return new AuditContextHolder();
    }

    @Bean
    @ConditionalOnMissingBean
    public List<FieldMasker> fieldMaskers() {
        return List.of(
                new PasswordFieldMasker(),
                new CreditCardFieldMasker(),
                new EmailFieldMasker()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public PayloadProcessor payloadProcessor(
            ObjectMapper objectMapper,
            AuditProperties properties,
            List<FieldMasker> maskers) {
        return new PayloadProcessor(objectMapper, properties, maskers);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditMetrics auditMetrics(MeterRegistry registry) {
        return new AuditMetrics(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditAspect auditAspect(
            AuditLogRepository repository,
            PayloadProcessor processor,
            AuditContextHolder contextHolder,
            AuditMetrics metrics,
            AuditProperties properties) {

        // Set default service name if not configured
        if (properties.getServiceName() == null || properties.getServiceName().isBlank()) {
            properties.setServiceName(applicationName);
        }

        return new AuditAspect(repository, processor, contextHolder, metrics, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditHealthIndicator auditHealthIndicator(AuditLogRepository repository) {
        return new AuditHealthIndicator(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditQueryService auditQueryService(AuditLogRepository repository) {
        return new AuditQueryService(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditQueryController auditQueryController(AuditQueryService queryService) {
        return new AuditQueryController(queryService);
    }

    // ========== Domain Event based audit beans ==========

    @Bean
    @ConditionalOnMissingBean
    public AuditEventPublisher auditEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            AuditProperties properties) {
        return new SpringAuditEventPublisher(applicationEventPublisher, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditDomainEventListener auditDomainEventListener(
            AuditLogRepository repository,
            PayloadProcessor payloadProcessor,
            AuditMetrics metrics) {
        return new AuditDomainEventListener(repository, payloadProcessor, metrics);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditEventBuilder auditEventBuilder(
            AuditContextHolder contextHolder,
            AuditProperties properties,
            ObjectMapper objectMapper) {

        // Set default service name if not configured
        if (properties.getServiceName() == null || properties.getServiceName().isBlank()) {
            properties.setServiceName(applicationName);
        }

        return new AuditEventBuilder(contextHolder, properties, objectMapper);
    }
}
