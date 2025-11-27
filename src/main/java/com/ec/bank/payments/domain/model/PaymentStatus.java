package com.ec.bank.payments.domain.model;


/**
 * Represents the lifecycle status of a payment order.
 * Aligned with BIAN Payment Initiation status model.
 */
public enum PaymentStatus {
    /**
     * Payment order has been created but not yet processed.
     */
    PENDING,

    /**
     * Payment order is currently being processed.
     */
    PROCESSING,

    /**
     * Payment has been successfully completed and settled.
     */
    SETTLED,

    /**
     * Payment order has been rejected due to validation or business rules.
     */
    REJECTED,

    /**
     * Payment order has been cancelled before completion.
     */
    CANCELLED;

    /**
     * Checks if the status represents a final state.
     */
    public boolean isFinal() {
        return this == SETTLED || this == REJECTED || this == CANCELLED;
    }

    /**
     * Checks if the status allows cancellation.
     */
    public boolean canBeCancelled() {
        return this == PENDING;
    }
}
