-- Flyway migration: Create audit_logs table
-- Version: V1
-- Feature: 001-shared-audit-lib

CREATE TABLE audit_logs (
    id              UUID            PRIMARY KEY,
    timestamp       TIMESTAMP       NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    aggregate_type  VARCHAR(100)    NOT NULL,
    aggregate_id    VARCHAR(255),
    username        VARCHAR(100)    NOT NULL DEFAULT 'ANONYMOUS',
    service_name    VARCHAR(100)    NOT NULL,
    action          VARCHAR(255),
    payload         TEXT,
    result          VARCHAR(20)     NOT NULL,
    error_message   TEXT,
    client_ip       VARCHAR(45),
    correlation_id  VARCHAR(100),
    payload_truncated BOOLEAN       NOT NULL DEFAULT FALSE,

    -- Constraints
    CONSTRAINT chk_result CHECK (result IN ('SUCCESS', 'FAILURE'))
);

-- Comment on table
COMMENT ON TABLE audit_logs IS 'Append-only audit log table for tracking all audited operations';
COMMENT ON COLUMN audit_logs.id IS 'Unique identifier for the audit entry (UUID)';
COMMENT ON COLUMN audit_logs.timestamp IS 'When the audited operation occurred (UTC)';
COMMENT ON COLUMN audit_logs.event_type IS 'Type of audit event (e.g., PRODUCT_CREATED)';
COMMENT ON COLUMN audit_logs.aggregate_type IS 'Entity type being audited (e.g., Product, User)';
COMMENT ON COLUMN audit_logs.aggregate_id IS 'ID of the entity being audited';
COMMENT ON COLUMN audit_logs.username IS 'Identity of the user who performed the operation';
COMMENT ON COLUMN audit_logs.service_name IS 'Name of the microservice that generated this audit';
COMMENT ON COLUMN audit_logs.action IS 'Method or operation name';
COMMENT ON COLUMN audit_logs.payload IS 'JSON serialized operation payload (max 64KB)';
COMMENT ON COLUMN audit_logs.result IS 'Operation result: SUCCESS or FAILURE';
COMMENT ON COLUMN audit_logs.error_message IS 'Error details if operation failed';
COMMENT ON COLUMN audit_logs.client_ip IS 'IP address of the client';
COMMENT ON COLUMN audit_logs.correlation_id IS 'ID linking related operations in a transaction';
COMMENT ON COLUMN audit_logs.payload_truncated IS 'True if payload was truncated due to size limit';
