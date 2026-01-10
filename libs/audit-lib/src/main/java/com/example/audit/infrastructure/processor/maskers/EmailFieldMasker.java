package com.example.audit.infrastructure.processor.maskers;

import com.example.audit.infrastructure.processor.FieldMasker;

import java.util.regex.Pattern;

/**
 * Field masker for email addresses.
 *
 * <p>Masks email addresses while preserving partial visibility of the local part
 * and the full domain for identification purposes.</p>
 */
public class EmailFieldMasker implements FieldMasker {

    private static final String FULLY_MASKED = "***@***.***";
    private static final int VISIBLE_CHARS = 2;

    private static final Pattern FIELD_PATTERN = Pattern.compile(
            ".*(email|e.?mail).*",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String mask(String value) {
        if (value == null || value.isBlank()) {
            return FULLY_MASKED;
        }

        int atIndex = value.indexOf('@');
        if (atIndex < 0) {
            return FULLY_MASKED;
        }

        String localPart = value.substring(0, atIndex);
        String domain = value.substring(atIndex + 1);

        // Show first N characters of local part, then mask
        String visiblePart;
        if (localPart.length() <= VISIBLE_CHARS) {
            visiblePart = localPart;
        } else {
            visiblePart = localPart.substring(0, VISIBLE_CHARS);
        }

        return visiblePart + "***@" + domain;
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
        return 50;
    }
}
