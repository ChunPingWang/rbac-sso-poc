package com.example.audit.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Metrics collector for audit operations.
 *
 * <p>Exposes the following metrics:</p>
 * <ul>
 *   <li>audit.events.total - Total number of audit events captured</li>
 *   <li>audit.events.failed - Number of failed audit captures</li>
 *   <li>audit.capture.latency - Time to capture and store audit logs</li>
 * </ul>
 */
@Component
public class AuditMetrics {

    private static final String METRIC_PREFIX = "audit";

    private final Counter totalCounter;
    private final Counter failedCounter;
    private final Timer captureLatencyTimer;

    public AuditMetrics(MeterRegistry registry) {
        this.totalCounter = Counter.builder(METRIC_PREFIX + ".events.total")
                .description("Total number of audit events captured")
                .register(registry);

        this.failedCounter = Counter.builder(METRIC_PREFIX + ".events.failed")
                .description("Number of failed audit capture attempts")
                .register(registry);

        this.captureLatencyTimer = Timer.builder(METRIC_PREFIX + ".capture.latency")
                .description("Time to capture and store audit log")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(registry);
    }

    /**
     * Increments the total events counter.
     */
    public void incrementTotal() {
        totalCounter.increment();
    }

    /**
     * Increments the failed events counter.
     */
    public void incrementFailed() {
        failedCounter.increment();
    }

    /**
     * Records the latency of an audit capture operation.
     *
     * @param latencyMs the latency in milliseconds
     */
    public void recordLatency(long latencyMs) {
        captureLatencyTimer.record(latencyMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Gets the total number of events captured.
     *
     * @return the total count
     */
    public double getTotalCount() {
        return totalCounter.count();
    }

    /**
     * Gets the number of failed captures.
     *
     * @return the failed count
     */
    public double getFailedCount() {
        return failedCounter.count();
    }
}
