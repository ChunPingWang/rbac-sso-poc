package com.example.audit.integration;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Test application for integration tests.
 *
 * <p>Uses minimal configuration to avoid bean conflicts.
 * AuditAutoConfiguration is loaded automatically via AutoConfiguration.imports.</p>
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class TestApplication {
}
