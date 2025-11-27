package com.ec.bank.payments.domain.model;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate Root representing a Payment Order.
 * Contains all business logic and validation rules.
 * Immutable - state changes create new instances.
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentOrder {

    private final PaymentOrderId paymentOrderId;
    private final String paymentOrderReference;
    private final IBAN debtorAccount;
    private final IBAN creditorAccount;
    private final Amount instructedAmount;
    private final String remittanceInformation;
    private final LocalDate requestedExecutionDateTime;
    private final PaymentStatus status;
    private final Instant createdAt;
    private final Instant lastUpdatedAt;

    /**
     * Creates a new Payment Order (factory method).
     */
    public static PaymentOrder create(
            String paymentOrderReference,
            IBAN debtorAccount,
            IBAN creditorAccount,
            Amount instructedAmount,
            String remittanceInformation,
            LocalDate requestedExecutionDateTime) {

        validateCreation(debtorAccount, creditorAccount, requestedExecutionDateTime);

        Instant now = Instant.now();

        return PaymentOrder.builder()
                .paymentOrderId(PaymentOrderId.generate())
                .paymentOrderReference(paymentOrderReference)
                .debtorAccount(debtorAccount)
                .creditorAccount(creditorAccount)
                .instructedAmount(instructedAmount)
                .remittanceInformation(remittanceInformation)
                .requestedExecutionDateTime(requestedExecutionDateTime)
                .status(PaymentStatus.PENDING)
                .createdAt(now)
                .lastUpdatedAt(now)
                .build();
    }

    /**
     * Validates business rules for payment order creation.
     */
    private static void validateCreation(
            IBAN debtorAccount,
            IBAN creditorAccount,
            LocalDate requestedExecutionDateTime) {

        Objects.requireNonNull(debtorAccount, "Debtor account is required");
        Objects.requireNonNull(creditorAccount, "Creditor account is required");
        Objects.requireNonNull(requestedExecutionDateTime, "Execution date is required");

        if (debtorAccount.equals(creditorAccount)) {
            throw new IllegalArgumentException(
                    "Debtor and creditor accounts cannot be the same");
        }

        if (requestedExecutionDateTime.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Execution date cannot be in the past");
        }
    }

    /**
     * Transitions the payment order to PROCESSING status.
     */
    public PaymentOrder markAsProcessing() {
        validateStatusTransition(PaymentStatus.PROCESSING);
        return updateStatus(PaymentStatus.PROCESSING);
    }

    /**
     * Transitions the payment order to SETTLED status.
     */
    public PaymentOrder markAsSettled() {
        validateStatusTransition(PaymentStatus.SETTLED);
        return updateStatus(PaymentStatus.SETTLED);
    }

    /**
     * Transitions the payment order to REJECTED status.
     */
    public PaymentOrder markAsRejected() {
        validateStatusTransition(PaymentStatus.REJECTED);
        return updateStatus(PaymentStatus.REJECTED);
    }

    /**
     * Cancels the payment order if allowed.
     */
    public PaymentOrder cancel() {
        if (!status.canBeCancelled()) {
            throw new IllegalStateException(
                    String.format("Cannot cancel payment order in status: %s", status));
        }
        return updateStatus(PaymentStatus.CANCELLED);
    }

    /**
     * Validates if status transition is allowed.
     */
    private void validateStatusTransition(PaymentStatus newStatus) {
        if (status.isFinal()) {
            throw new IllegalStateException(
                    String.format("Cannot transition from final status %s to %s", status, newStatus));
        }
    }

    /**
     * Creates a new instance with updated status.
     */
    private PaymentOrder updateStatus(PaymentStatus newStatus) {
        return this.toBuilder()
                .status(newStatus)
                .lastUpdatedAt(Instant.now())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentOrder that)) return false;
        return Objects.equals(paymentOrderId, that.paymentOrderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentOrderId);
    }
}
