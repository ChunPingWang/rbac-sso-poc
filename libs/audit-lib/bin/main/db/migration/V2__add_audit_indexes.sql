-- Flyway migration: Add performance indexes for audit log queries
-- Version: V2
-- Feature: 001-shared-audit-lib
-- Per SC-008: <5 seconds query time for 1M records

-- Index for queries by timestamp (most common - default sort)
CREATE INDEX IF NOT EXISTS idx_audit_timestamp
    ON audit_logs(timestamp DESC);

-- Index for queries by username + timestamp
CREATE INDEX IF NOT EXISTS idx_audit_username
    ON audit_logs(username, timestamp DESC);

-- Index for queries by aggregate (entity) type and ID
CREATE INDEX IF NOT EXISTS idx_audit_aggregate
    ON audit_logs(aggregate_type, aggregate_id, timestamp DESC);

-- Index for queries by event type
CREATE INDEX IF NOT EXISTS idx_audit_event_type
    ON audit_logs(event_type, timestamp DESC);

-- Index for queries by service name
CREATE INDEX IF NOT EXISTS idx_audit_service
    ON audit_logs(service_name, timestamp DESC);

-- Partial index for correlation ID (only non-null values)
CREATE INDEX IF NOT EXISTS idx_audit_correlation
    ON audit_logs(correlation_id)
    WHERE correlation_id IS NOT NULL;

-- Index for result filtering (SUCCESS/FAILURE)
CREATE INDEX IF NOT EXISTS idx_audit_result
    ON audit_logs(result, timestamp DESC);
