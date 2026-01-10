package com.example.audit.infrastructure.processor.maskers;

import com.example.audit.infrastructure.processor.FieldMasker;

import java.util.regex.Pattern;

/**
 * Field masker for password and secret fields.
 *
 * <p>Completely masks password values with asterisks for security.</p>
 */
public class PasswordFieldMasker implements FieldMasker {

    private static final String MASKED_VALUE = "********";

    private static final Pattern FIELD_PATTERN = Pattern.compile(
            ".*(password|secret|credential|token|apikey|api_key).*",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String mask(String value) {
        // Always return masked value regardless of input
        return MASKED_VALUE;
    }

    @Override
    public boolean supports(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        return FIELD_PATTERN.matcher(fieldName).matches();
    }

    @Override
    public int getPriority() {
        // High priority - passwords should be masked first
        return 100;
    }
}
