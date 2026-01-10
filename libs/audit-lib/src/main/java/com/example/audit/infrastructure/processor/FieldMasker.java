package com.example.audit.infrastructure.processor;

/**
 * Interface for field masking strategies.
 *
 * <p>Implementations provide specific masking logic for different types of
 * sensitive data (passwords, credit cards, emails, etc.).</p>
 */
public interface FieldMasker {

    /**
     * Masks the given value.
     *
     * @param value the value to mask (may be null)
     * @return the masked value
     */
    String mask(String value);

    /**
     * Checks if this masker supports the given field name.
     *
     * @param fieldName the field name to check
     * @return true if this masker should handle this field
     */
    boolean supports(String fieldName);

    /**
     * Returns the priority of this masker.
     * Higher priority maskers are checked first.
     *
     * @return the priority (default 0)
     */
    default int getPriority() {
        return 0;
    }
}
