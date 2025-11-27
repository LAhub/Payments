package com.ec.bank.payments;


import com.ec.bank.payments.domain.model.Amount;
import com.ec.bank.payments.domain.model.IBAN;
import com.ec.bank.payments.domain.model.PaymentOrder;
import com.ec.bank.payments.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PaymentOrder domain entity.
 */
@DisplayName("PaymentOrder Domain Entity Tests")
class PaymentOrderTest {

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create valid payment order with all required fields")
        void shouldCreateValidPaymentOrder() {
            // Given
            String reference = "REF-2025-001";
            IBAN debtorIban = IBAN.of("ES7921000813610123456789");
            IBAN creditorIban = IBAN.of("ES1420805801101234567891");
            Amount amount = Amount.of(BigDecimal.valueOf(1500.00), "EUR");
            String remittance = "Invoice payment";
            LocalDate executionDate = LocalDate.now().plusDays(1);

            // When
            PaymentOrder order = PaymentOrder.create(
                    reference, debtorIban, creditorIban, amount, remittance, executionDate
            );

            // Then
            assertThat(order).isNotNull();
            assertThat(order.getPaymentOrderId()).isNotNull();
            assertThat(order.getPaymentOrderReference()).isEqualTo(reference);
            assertThat(order.getDebtorAccount()).isEqualTo(debtorIban);
            assertThat(order.getCreditorAccount()).isEqualTo(creditorIban);
            assertThat(order.getInstructedAmount()).isEqualTo(amount);
            assertThat(order.getRemittanceInformation()).isEqualTo(remittance);
            assertThat(order.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(order.getCreatedAt()).isNotNull();
            assertThat(order.getLastUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should fail when debtor and creditor accounts are the same")
        void shouldFailWhenSameAccounts() {
            // Given
            IBAN sameIban = IBAN.of("ES7921000813610123456789");
            Amount amount = Amount.of(BigDecimal.valueOf(1500.00), "EUR");
            LocalDate executionDate = LocalDate.now().plusDays(1);

            // When/Then
            assertThatThrownBy(() -> PaymentOrder.create(
                    "REF-001", sameIban, sameIban, amount, "Test", executionDate
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be the same");
        }

        @Test
        @DisplayName("Should fail when execution date is in the past")
        void shouldFailWhenExecutionDateInPast() {
            // Given
            IBAN debtorIban = IBAN.of("ES7921000813610123456789");
            IBAN creditorIban = IBAN.of("ES1420805801101234567891");
            Amount amount = Amount.of(BigDecimal.valueOf(1500.00), "EUR");
            LocalDate pastDate = LocalDate.now().minusDays(1);

            // When/Then
            assertThatThrownBy(() -> PaymentOrder.create(
                    "REF-001", debtorIban, creditorIban, amount, "Test", pastDate
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be in the past");
        }
    }

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {

        private PaymentOrder createTestOrder() {
            return PaymentOrder.create(
                    "REF-001",
                    IBAN.of("ES7921000813610123456789"),
                    IBAN.of("ES1420805801101234567891"),
                    Amount.of(BigDecimal.valueOf(1500.00), "EUR"),
                    "Test payment",
                    LocalDate.now().plusDays(1)
            );
        }

        @Test
        @DisplayName("Should transition from PENDING to PROCESSING")
        void shouldTransitionToProcessing() {
            // Given
            PaymentOrder order = createTestOrder();

            // When
            PaymentOrder processed = order.markAsProcessing();

            // Then
            assertThat(processed.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
            assertThat(processed.getLastUpdatedAt()).isAfter(order.getLastUpdatedAt());
        }

        @Test
        @DisplayName("Should transition from PROCESSING to SETTLED")
        void shouldTransitionToSettled() {
            // Given
            PaymentOrder order = createTestOrder().markAsProcessing();

            // When
            PaymentOrder settled = order.markAsSettled();

            // Then
            assertThat(settled.getStatus()).isEqualTo(PaymentStatus.SETTLED);
        }

        @Test
        @DisplayName("Should not transition from SETTLED to any other status")
        void shouldNotTransitionFromFinalStatus() {
            // Given
            PaymentOrder order = createTestOrder()
                    .markAsProcessing()
                    .markAsSettled();

            // When/Then
            assertThatThrownBy(order::markAsProcessing)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot transition from final status");
        }

        @Test
        @DisplayName("Should cancel order when in PENDING status")
        void shouldCancelPendingOrder() {
            // Given
            PaymentOrder order = createTestOrder();

            // When
            PaymentOrder cancelled = order.cancel();

            // Then
            assertThat(cancelled.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should not cancel order when not in PENDING status")
        void shouldNotCancelProcessingOrder() {
            // Given
            PaymentOrder order = createTestOrder().markAsProcessing();

            // When/Then
            assertThatThrownBy(order::cancel)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel");
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Two orders with same ID should be equal")
        void shouldBeEqualWithSameId() {
            // Given
            PaymentOrder order1 = PaymentOrder.create(
                    "REF-001",
                    IBAN.of("ES7921000813610123456789"),
                    IBAN.of("ES1420805801101234567891"),
                    Amount.of(BigDecimal.valueOf(1500.00), "EUR"),
                    "Test",
                    LocalDate.now().plusDays(1)
            );

            PaymentOrder order2 = order1.toBuilder().build();

            // Then
            assertThat(order1).isEqualTo(order2);
            assertThat(order1.hashCode()).isEqualTo(order2.hashCode());
        }
    }
}
