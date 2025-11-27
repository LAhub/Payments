package com.ec.bank.payments;

import com.ec.bank.payments.domain.model.Amount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Amount value object.
 */
@DisplayName("Amount Value Object Tests")
class AmountTest {

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create valid amount")
        void shouldCreateValidAmount() {
            // When
            Amount amount = Amount.of(BigDecimal.valueOf(1500.00), "EUR");

            // Then
            assertThat(amount.value()).isEqualByComparingTo(BigDecimal.valueOf(1500.00));
            assertThat(amount.currencyCode()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("Should normalize amount to 2 decimal places")
        void shouldNormalizeDecimals() {
            // When
            Amount amount = Amount.of(BigDecimal.valueOf(1500.12345), "EUR");

            // Then
            assertThat(amount.value()).isEqualByComparingTo(BigDecimal.valueOf(1500.12));
        }

        @Test
        @DisplayName("Should fail with null value")
        void shouldFailWithNullValue() {
            // When/Then
            assertThatThrownBy(() -> Amount.of(null, "EUR"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Amount value cannot be null");
        }

        @Test
        @DisplayName("Should fail with negative amount")
        void shouldFailWithNegativeAmount() {
            // When/Then
            assertThatThrownBy(() -> Amount.of(BigDecimal.valueOf(-100), "EUR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must be positive");
        }

        @Test
        @DisplayName("Should fail with zero amount")
        void shouldFailWithZeroAmount() {
            // When/Then
            assertThatThrownBy(() -> Amount.of(BigDecimal.ZERO, "EUR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount must be positive");
        }

        @Test
        @DisplayName("Should fail with invalid currency code")
        void shouldFailWithInvalidCurrency() {
            // When/Then
            assertThatThrownBy(() -> Amount.of(BigDecimal.valueOf(100), "INVALID"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Comparison Tests")
    class ComparisonTests {

        @Test
        @DisplayName("Should correctly compare amounts")
        void shouldCompareAmounts() {
            // Given
            Amount amount1 = Amount.of(BigDecimal.valueOf(1500), "EUR");
            Amount amount2 = Amount.of(BigDecimal.valueOf(1000), "EUR");

            // Then
            assertThat(amount1.isGreaterThan(amount2)).isTrue();
            assertThat(amount2.isGreaterThan(amount1)).isFalse();
        }

        @Test
        @DisplayName("Should fail comparing different currencies")
        void shouldFailComparingDifferentCurrencies() {
            // Given
            Amount eurAmount = Amount.of(BigDecimal.valueOf(1500), "EUR");
            Amount usdAmount = Amount.of(BigDecimal.valueOf(1000), "USD");

            // When/Then
            assertThatThrownBy(() -> eurAmount.isGreaterThan(usdAmount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("different currencies");
        }
    }
}
