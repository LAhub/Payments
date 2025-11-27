package com.ec.bank.payments.domain.model;


import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a unique Payment Order identifier.
 * Immutable and self-validating.
 */
public record PaymentOrderId(String value) {

    public PaymentOrderId {
        Objects.requireNonNull(value, "Payment Order ID cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Payment Order ID cannot be blank");
        }
    }

    /**
     * Generates a new unique Payment Order ID.
     */
    public static PaymentOrderId generate() {
        return new PaymentOrderId("PO-" + UUID.randomUUID().toString());
    }

    /**
     * Creates a PaymentOrderId from an existing value.
     */
    public static PaymentOrderId of(String value) {
        return new PaymentOrderId(value);
    }
}