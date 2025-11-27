package com.ec.bank.payments.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object representing a monetary amount with currency.
 * Immutable and ensures positive amounts.
 */
public record Amount(BigDecimal value, Currency currency) {

    public Amount {
        Objects.requireNonNull(value, "Amount value cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Normalize to 2 decimal places
        value = value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Creates an Amount from value and currency code.
     */
    public static Amount of(BigDecimal value, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        return new Amount(value, currency);
    }

    /**
     * Creates an Amount from double and currency code.
     */
    public static Amount of(double value, String currencyCode) {
        return of(BigDecimal.valueOf(value), currencyCode);
    }

    /**
     * Returns the currency code (e.g., "EUR", "USD").
     */
    public String currencyCode() {
        return currency.getCurrencyCode();
    }

    /**
     * Checks if this amount is greater than another.
     */
    public boolean isGreaterThan(Amount other) {
        ensureSameCurrency(other);
        return this.value.compareTo(other.value) > 0;
    }

    private void ensureSameCurrency(Amount other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    String.format("Cannot compare amounts with different currencies: %s vs %s",
                            this.currency, other.currency)
            );
        }
    }
}