package com.example.audit.infrastructure.web;

import com.example.audit.application.dto.AuditLogView;
import com.example.audit.application.dto.PagedResponse;
import com.example.audit.application.service.AuditQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for audit log queries.
 *
 * <p>Provides read-only access to audit logs for compliance and security review.
 * Per the API contract (audit-api.yaml), this controller only supports GET operations.</p>
 *
 * <p>Access Control:</p>
 * <ul>
 *   <li>All endpoints require authentication</li>
 *   <li>Requires ADMIN or AUDITOR role</li>
 *   <li>Service accounts with audit:read scope are also permitted</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR') or hasAuthority('SCOPE_audit:read')")
public class AuditQueryController {

    private final AuditQueryService queryService;

    public AuditQueryController(AuditQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * Query audit logs with optional filters.
     *
     * @param username      filter by executor username
     * @param aggregateType filter by aggregate/entity type
     * @param aggregateId   filter by aggregate/entity ID (requires aggregateType)
     * @param eventType     filter by event type
     * @param serviceName   filter by originating service
     * @param startTime     filter by start time (inclusive)
     * @param endTime       filter by end time (exclusive)
     * @param page          page number (0-indexed, default 0)
     * @param size          page size (default 20, max 100)
     * @return paginated audit logs
     */
    @GetMapping
    public ResponseEntity<PagedResponse<AuditLogView>> queryAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String aggregateType,
            @RequestParam(required = false) String aggregateId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Clamp size to max 100
        size = Math.min(size, 100);

        // Route to appropriate query method based on provided filters
        PagedResponse<AuditLogView> result;

        if (username != null && !username.isBlank()) {
            result = queryService.findByUsername(username, page, size);
        } else if (aggregateType != null && aggregateId != null) {
            result = queryService.findByAggregate(aggregateType, aggregateId, page, size);
        } else if (eventType != null && !eventType.isBlank()) {
            result = queryService.findByEventType(eventType, page, size);
        } else if (serviceName != null && !serviceName.isBlank()) {
            result = queryService.findByServiceName(serviceName, page, size);
        } else if (startTime != null && endTime != null) {
            result = queryService.findByTimeRange(startTime, endTime, page, size);
        } else if (startTime != null) {
            result = queryService.findByTimeRange(startTime, Instant.now(), page, size);
        } else {
            // Default: return recent logs
            result = queryService.findByTimeRange(
                    Instant.now().minusSeconds(86400), // Last 24 hours
                    Instant.now(),
                    page, size);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get a single audit log by ID.
     *
     * @param id the audit log UUID
     * @return the audit log if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLogView> getAuditLogById(@PathVariable UUID id) {
        return queryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all audit logs related by correlation ID.
     *
     * @param correlationId the correlation ID
     * @return list of related audit logs
     */
    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<List<AuditLogView>> getByCorrelationId(
            @PathVariable String correlationId) {
        List<AuditLogView> result = queryService.findByCorrelationId(correlationId);
        return ResponseEntity.ok(result);
    }
}
