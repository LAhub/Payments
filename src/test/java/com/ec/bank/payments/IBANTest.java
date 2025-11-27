package com.ec.bank.payments;


import com.ec.bank.payments.domain.model.IBAN;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for IBAN value object.
 */
@DisplayName("IBAN Value Object Tests")
class IBANTest {

    @ParameterizedTest
    @DisplayName("Should accept valid IBAN formats")
    @ValueSource(strings = {
            "ES7921000813610123456789",
            "GB82WEST12345698765432",
            "DE89370400440532013000",
            "FR1420041010050500013M02606"
    })
    void shouldAcceptValidIbans(String validIban) {
        // When
        IBAN iban = IBAN.of(validIban);

        // Then
        assertThat(iban.value()).isEqualTo(validIban);
    }

    @Test
    @DisplayName("Should normalize IBAN by removing spaces")
    void shouldNormalizeIban() {
        // Given
        String ibanWithSpaces = "ES79 2100 0813 6101 2345 6789";

        // When
        IBAN iban = IBAN.of(ibanWithSpaces);

        // Then
        assertThat(iban.value()).isEqualTo("ES7921000813610123456789");
    }

    @ParameterizedTest
    @DisplayName("Should reject invalid IBAN formats")
    @ValueSource(strings = {
            "INVALID",
            "ES79",
            "1234567890",
            "ES7921000813610123456789TOOLONG123456789"
    })
    void shouldRejectInvalidIbans(String invalidIban) {
        // When/Then
        assertThatThrownBy(() -> IBAN.of(invalidIban))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid IBAN format");
    }

    @Test
    @DisplayName("Should fail with null value")
    void shouldFailWithNull() {
        // When/Then
        assertThatThrownBy(() -> IBAN.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("IBAN cannot be null");
    }

    @Test
    @DisplayName("Should format IBAN with spaces")
    void shouldFormatIban() {
        // Given
        IBAN iban = IBAN.of("ES7921000813610123456789");

        // When
        String formatted = iban.formatted();

        // Then
        assertThat(formatted).isEqualTo("ES79 2100 0813 6101 2345 6789");
    }
}
