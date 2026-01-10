package com.example.audit.domain.model;

/**
 * Represents the result of an audited operation.
 */
public enum AuditResult {
    /**
     * Operation completed successfully.
     */
    SUCCESS,

    /**
     * Operation failed with an error.
     */
    FAILURE
}
