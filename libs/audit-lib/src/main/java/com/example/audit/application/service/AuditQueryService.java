package com.example.audit.application.service;

import com.example.audit.application.dto.AuditLogView;
import com.example.audit.application.dto.PagedResponse;
import com.example.audit.domain.model.AuditEventType;
import com.example.audit.domain.model.AuditLog;
import com.example.audit.domain.model.AuditLogId;
import com.example.audit.domain.port.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for querying audit logs.
 *
 * <p>Provides paginated query methods for various filter criteria.</p>
 */
@Service
@Transactional(readOnly = true)
public class AuditQueryService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "timestamp");

    private final AuditLogRepository repository;

    public AuditQueryService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Gets an audit log by its ID.
     *
     * @param id the audit log ID
     * @return the audit log view if found
     */
    public Optional<AuditLogView> findById(UUID id) {
        return repository.findById(AuditLogId.of(id))
                .map(AuditLogView::from);
    }

    /**
     * Queries audit logs by username with pagination.
     *
     * @param username the username to filter by
     * @param page     page number (0-indexed)
     * @param size     page size
     * @return paginated audit logs
     */
    public PagedResponse<AuditLogView> findByUsername(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<AuditLogView> result = repository.findByUsername(username, pageable)
                .map(AuditLogView::from);
        return PagedResponse.from(result);
    }

    /**
     * Queries audit logs by event type with pagination.
     *
     * @param eventType the event type to filter by
     * @param page      page number (0-indexed)
     * @param size      page size
     * @return paginated audit logs
     */
    public PagedResponse<AuditLogView> findByEventType(String eventType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<AuditLogView> result = repository.findByEventType(AuditEventType.of(eventType), pageable)
                .map(AuditLogView::from);
        return PagedResponse.from(result);
    }

    /**
     * Queries audit logs by aggregate (entity) type and ID with pagination.
     *
     * @param aggregateType the aggregate type
     * @param aggregateId   the aggregate ID
     * @param page          page number (0-indexed)
     * @param size          page size
     * @return paginated audit logs
     */
    public PagedResponse<AuditLogView> findByAggregate(
            String aggregateType, String aggregateId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<AuditLogView> result = repository.findByAggregateTypeAndAggregateId(
                        aggregateType, aggregateId, pageable)
                .map(AuditLogView::from);
        return PagedResponse.from(result);
    }

    /**
     * Queries audit logs by time range with pagination.
     *
     * @param startTime start of time range (inclusive)
     * @param endTime   end of time range (exclusive)
     * @param page      page number (0-indexed)
     * @param size      page size
     * @return paginated audit logs
     */
    public PagedResponse<AuditLogView> findByTimeRange(
            Instant startTime, Instant endTime, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<AuditLogView> result = repository.findByTimestampBetween(startTime, endTime, pageable)
                .map(AuditLogView::from);
        return PagedResponse.from(result);
    }

    /**
     * Queries audit logs by service name with pagination.
     *
     * @param serviceName the service name to filter by
     * @param page        page number (0-indexed)
     * @param size        page size
     * @return paginated audit logs
     */
    public PagedResponse<AuditLogView> findByServiceName(String serviceName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<AuditLogView> result = repository.findByServiceName(serviceName, pageable)
                .map(AuditLogView::from);
        return PagedResponse.from(result);
    }

    /**
     * Gets all audit logs related by correlation ID.
     *
     * @param correlationId the correlation ID linking related operations
     * @return list of related audit logs
     */
    public List<AuditLogView> findByCorrelationId(String correlationId) {
        return repository.findByCorrelationId(correlationId).stream()
                .map(AuditLogView::from)
                .toList();
    }
}
