package com.example.audit.domain.port;

import com.example.audit.domain.model.AuditEventType;
import com.example.audit.domain.model.AuditLog;
import com.example.audit.domain.model.AuditLogId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Output port for audit log persistence.
 *
 * <p>This interface intentionally omits update() and delete() methods to enforce
 * append-only semantics per FR-011. Audit logs are immutable once created.</p>
 */
public interface AuditLogRepository {

    /**
     * Saves a new audit log entry.
     *
     * @param auditLog the audit log to save
     * @return the saved audit log
     */
    AuditLog save(AuditLog auditLog);

    /**
     * Finds an audit log by its unique identifier.
     *
     * @param id the audit log ID
     * @return the audit log if found
     */
    Optional<AuditLog> findById(AuditLogId id);

    /**
     * Finds audit logs by username with pagination.
     *
     * @param username the username to search for
     * @param pageable pagination parameters
     * @return a page of audit logs
     */
    Page<AuditLog> findByUsername(String username, Pageable pageable);

    /**
     * Finds audit logs by aggregate type and ID with pagination.
     *
     * @param aggregateType the aggregate type
     * @param aggregateId   the aggregate ID
     * @param pageable      pagination parameters
     * @return a page of audit logs
     */
    Page<AuditLog> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId, Pageable pageable);

    /**
     * Finds audit logs by event type with pagination.
     *
     * @param eventType the event type to search for
     * @param pageable  pagination parameters
     * @return a page of audit logs
     */
    Page<AuditLog> findByEventType(AuditEventType eventType, Pageable pageable);

    /**
     * Finds audit logs within a time range with pagination.
     *
     * @param startTime the start of the time range (inclusive)
     * @param endTime   the end of the time range (exclusive)
     * @param pageable  pagination parameters
     * @return a page of audit logs
     */
    Page<AuditLog> findByTimestampBetween(Instant startTime, Instant endTime, Pageable pageable);

    /**
     * Finds audit logs by service name with pagination.
     *
     * @param serviceName the service name to search for
     * @param pageable    pagination parameters
     * @return a page of audit logs
     */
    Page<AuditLog> findByServiceName(String serviceName, Pageable pageable);

    /**
     * Finds all audit logs sharing the same correlation ID.
     * Used to retrieve related operations within a business transaction.
     *
     * @param correlationId the correlation ID linking related operations
     * @return a list of related audit logs
     */
    List<AuditLog> findByCorrelationId(String correlationId);

    /**
     * Checks if an audit log exists with the given ID.
     *
     * @param id the audit log ID
     * @return true if the audit log exists
     */
    boolean existsById(AuditLogId id);
}
