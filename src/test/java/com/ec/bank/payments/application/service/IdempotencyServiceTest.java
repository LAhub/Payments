package com.ec.bank.payments.application.service;


import com.ec.bank.payments.domain.exception.DuplicatePaymentOrderException;
import com.ec.bank.payments.domain.port.output.IdempotencyRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

/**
 * Unit tests for IdempotencyService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyService Tests")
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRepository idempotencyRepository;

    private MeterRegistry meterRegistry;
    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new IdempotencyService(idempotencyRepository, meterRegistry);
    }

    @Test
    @DisplayName("Should pass check when idempotency key does not exist")
    void shouldPassWhenKeyDoesNotExist() {
        // Given
        String idempotencyKey = "key-123";
        when(idempotencyRepository.exists(idempotencyKey))
                .thenReturn(Mono.just(false));

        // When/Then
        StepVerifier.create(service.checkIdempotency(idempotencyKey))
                .verifyComplete();

        verify(idempotencyRepository).exists(idempotencyKey);
    }

    @Test
    @DisplayName("Should fail when idempotency key already exists")
    void shouldFailWhenKeyExists() {
        // Given
        String idempotencyKey = "key-123";
        String existingOrderId = "PO-001";

        when(idempotencyRepository.exists(idempotencyKey))
                .thenReturn(Mono.just(true));
        when(idempotencyRepository.findPaymentOrderId(idempotencyKey))
                .thenReturn(Mono.just(existingOrderId));

        // When/Then
        StepVerifier.create(service.checkIdempotency(idempotencyKey))
                .expectError(DuplicatePaymentOrderException.class)
                .verify();

        verify(idempotencyRepository).exists(idempotencyKey);
        verify(idempotencyRepository).findPaymentOrderId(idempotencyKey);
    }

    @Test
    @DisplayName("Should successfully save idempotency key")
    void shouldSaveIdempotencyKey() {
        // Given
        String idempotencyKey = "key-123";
        String paymentOrderId = "PO-001";

        when(idempotencyRepository.save(idempotencyKey, paymentOrderId))
                .thenReturn(Mono.empty());

        // When/Then
        StepVerifier.create(service.saveIdempotencyKey(idempotencyKey, paymentOrderId))
                .verifyComplete();

        verify(idempotencyRepository).save(idempotencyKey, paymentOrderId);
    }

    @Test
    @DisplayName("Should skip check when idempotency key is null")
    void shouldSkipCheckWhenKeyIsNull() {
        // When/Then
        StepVerifier.create(service.checkIdempotency(null))
                .verifyComplete();

        verifyNoInteractions(idempotencyRepository);
    }

    @Test
    @DisplayName("Should skip save when idempotency key is blank")
    void shouldSkipSaveWhenKeyIsBlank() {
        // When/Then
        StepVerifier.create(service.saveIdempotencyKey("", "PO-001"))
                .verifyComplete();

        verifyNoInteractions(idempotencyRepository);
    }
}
