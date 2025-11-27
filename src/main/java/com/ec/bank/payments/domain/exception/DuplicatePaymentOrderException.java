package com.ec.bank.payments.domain.exception;


/**
 * Exception thrown when attempting to create a duplicate payment order.
 */
public class DuplicatePaymentOrderException extends RuntimeException {

    private final String idempotencyKey;
    private final String existingPaymentOrderId;

    public DuplicatePaymentOrderException(String idempotencyKey, String existingPaymentOrderId) {
        super(String.format("Duplicate payment order detected. Idempotency key: %s, Existing order: %s",
                idempotencyKey, existingPaymentOrderId));
        this.idempotencyKey = idempotencyKey;
        this.existingPaymentOrderId = existingPaymentOrderId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getExistingPaymentOrderId() {
        return existingPaymentOrderId;
    }
}
