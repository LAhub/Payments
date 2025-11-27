package com.ec.bank.payments.application.service;


import com.ec.bank.payments.domain.exception.DuplicatePaymentOrderException;
import com.ec.bank.payments.domain.port.output.IdempotencyRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Application service for managing idempotency keys.
 * Ensures that duplicate requests are properly handled.
 */
@Slf4j
@Service
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final Counter duplicateRequestCounter;

    public IdempotencyService(
            IdempotencyRepository idempotencyRepository,
            MeterRegistry meterRegistry) {
        this.idempotencyRepository = idempotencyRepository;
        this.duplicateRequestCounter = Counter.builder("payment.idempotency.duplicate")
                .description("Number of duplicate payment requests detected")
                .tag("type", "payment-order")
                .register(meterRegistry);
    }

    /**
     * Checks if an idempotency key already exists.
     * If it exists, retrieves the existing payment order ID and throws exception.
     *
     * @param idempotencyKey The unique idempotency key
     * @return Mono that completes if key is unique, or errors if duplicate
     */
    public Mono<Void> checkIdempotency(String idempotencyKey) {
        if (Objects.isNull(idempotencyKey) || idempotencyKey.isBlank()) {
            log.warn("Idempotency key is null or blank, skipping check");
            return Mono.empty();
        }

        return idempotencyRepository.exists(idempotencyKey)
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Duplicate payment order detected with idempotency key: {}",
                                idempotencyKey);
                        duplicateRequestCounter.increment();

                        return idempotencyRepository.findPaymentOrderId(idempotencyKey)
                                .flatMap(existingId -> Mono.error(
                                        new DuplicatePaymentOrderException(idempotencyKey, existingId)
                                ));
                    }
                    return Mono.empty();
                })
                .doOnSuccess(v -> log.debug("Idempotency check passed for key: {}", idempotencyKey))
                .then();
    }

    /**
     * Saves the idempotency key with associated payment order ID.
     *
     * @param idempotencyKey The unique key
     * @param paymentOrderId The payment order ID
     * @return Mono that completes when saved
     */
    public Mono<Void> saveIdempotencyKey(String idempotencyKey, String paymentOrderId) {
        if (Objects.isNull(idempotencyKey) || idempotencyKey.isBlank()) {
            log.debug("Idempotency key is null or blank, skipping save");
            return Mono.empty();
        }

        return idempotencyRepository.save(idempotencyKey, paymentOrderId)
                .doOnSuccess(v -> log.debug("Saved idempotency key: {} -> {}",
                        idempotencyKey, paymentOrderId))
                .doOnError(e -> log.error("Failed to save idempotency key: {}",
                        idempotencyKey, e));
    }
}
