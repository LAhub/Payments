package com.ec.bank.payments.domain.model;


import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing an International Bank Account Number (IBAN).
 * Validates format according to ISO 13616.
 */
public record IBAN(String value) {

    private static final Pattern IBAN_PATTERN = Pattern.compile("^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$");

    public IBAN {
        Objects.requireNonNull(value, "IBAN cannot be null");
        String normalized = value.replaceAll("\\s+", "").toUpperCase();

        if (!IBAN_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid IBAN format: " + value);
        }

        value = normalized; // Use normalized value
    }

    /**
     * Creates an IBAN from string value.
     */
    public static IBAN of(String value) {
        return new IBAN(value);
    }

    /**
     * Returns the formatted IBAN with spaces every 4 characters.
     */
    public String formatted() {
        return value.replaceAll("(.{4})", "$1 ").trim();
    }
}
