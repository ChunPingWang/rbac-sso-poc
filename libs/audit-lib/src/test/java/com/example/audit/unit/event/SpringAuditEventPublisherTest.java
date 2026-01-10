package com.example.audit.unit.event;

import com.example.audit.domain.event.AuditableDomainEvent;
import com.example.audit.domain.event.BaseAuditEvent;
import com.example.audit.infrastructure.config.AuditProperties;
import com.example.audit.infrastructure.event.SpringAuditEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAuditEventPublisher Tests")
class SpringAuditEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private AuditProperties properties;
    private SpringAuditEventPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new AuditProperties();
        properties.setEnabled(true);
        publisher = new SpringAuditEventPublisher(applicationEventPublisher, properties);
    }

    @Nested
    @DisplayName("publish()")
    class PublishTests {

        @Test
        @DisplayName("should publish event when audit is enabled")
        void shouldPublishEventWhenAuditIsEnabled() {
            // given
            AuditableDomainEvent event = createTestEvent();

            // when
            publisher.publish(event);

            // then
            verify(applicationEventPublisher).publishEvent(event);
        }

        @Test
        @DisplayName("should not publish event when audit is disabled")
        void shouldNotPublishEventWhenAuditIsDisabled() {
            // given
            properties.setEnabled(false);
            AuditableDomainEvent event = createTestEvent();

            // when
            publisher.publish(event);

            // then
            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        @DisplayName("should not throw exception when publisher fails")
        void shouldNotThrowExceptionWhenPublisherFails() {
            // given
            AuditableDomainEvent event = createTestEvent();
            doThrow(new RuntimeException("Event bus error"))
                    .when(applicationEventPublisher).publishEvent(any());

            // when/then - should not throw
            assertThatNoException().isThrownBy(() -> publisher.publish(event));
        }
    }

    @Nested
    @DisplayName("publishSync()")
    class PublishSyncTests {

        @Test
        @DisplayName("should return true on successful publish")
        void shouldReturnTrueOnSuccessfulPublish() {
            // given
            AuditableDomainEvent event = createTestEvent();

            // when
            boolean result = publisher.publishSync(event);

            // then
            assertThat(result).isTrue();
            verify(applicationEventPublisher).publishEvent(event);
        }

        @Test
        @DisplayName("should return true when audit is disabled")
        void shouldReturnTrueWhenAuditIsDisabled() {
            // given
            properties.setEnabled(false);
            AuditableDomainEvent event = createTestEvent();

            // when
            boolean result = publisher.publishSync(event);

            // then
            assertThat(result).isTrue();
            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        @DisplayName("should return false when publisher fails")
        void shouldReturnFalseWhenPublisherFails() {
            // given
            AuditableDomainEvent event = createTestEvent();
            doThrow(new RuntimeException("Event bus error"))
                    .when(applicationEventPublisher).publishEvent(any());

            // when
            boolean result = publisher.publishSync(event);

            // then
            assertThat(result).isFalse();
        }
    }

    private AuditableDomainEvent createTestEvent() {
        return new BaseAuditEvent(
                "TEST_EVENT",
                "TestAggregate",
                "test-123",
                "testAction",
                "test@example.com",
                "test-service",
                "{\"test\":true}",
                true,
                null
        ) {};
    }
}
