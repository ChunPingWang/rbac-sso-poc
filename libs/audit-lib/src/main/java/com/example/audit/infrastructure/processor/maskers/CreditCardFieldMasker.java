package com.example.audit.infrastructure.processor.maskers;

import com.example.audit.infrastructure.processor.FieldMasker;

import java.util.regex.Pattern;

/**
 * Field masker for credit card numbers.
 *
 * <p>Masks credit card numbers while preserving the last 4 digits for
 * identification purposes, following PCI DSS guidelines.</p>
 */
public class CreditCardFieldMasker implements FieldMasker {

    private static final String FULLY_MASKED = "****-****-****-****";

    private static final Pattern FIELD_PATTERN = Pattern.compile(
            ".*(credit.?card|card.?number|cc.?number|pan|primaryaccountnumber).*",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public String mask(String value) {
        if (value == null || value.isBlank()) {
            return FULLY_MASKED;
        }

        // Remove any non-digit characters
        String digitsOnly = value.replaceAll("[^0-9]", "");

        if (digitsOnly.length() < 4) {
            return FULLY_MASKED;
        }

        // Get the last 4 digits
        String lastFour = digitsOnly.substring(digitsOnly.length() - 4);

        return "****-****-****-" + lastFour;
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
