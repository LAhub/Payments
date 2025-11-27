package com.ec.bank.payments.infrastructure.adapter.output.persistence.adapter;

import com.ec.bank.payments.domain.port.output.IdempotencyRepository;
import com.ec.bank.payments.infrastructure.adapter.output.persistence.entity.IdempotencyKeyEntity;
import com.ec.bank.payments.infrastructure.adapter.output.persistence.repository.R2dbcIdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Adapter implementing IdempotencyRepository port using R2DBC.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyRepositoryAdapter implements IdempotencyRepository {

    private static final long EXPIRATION_HOURS = 24;

    private final R2dbcIdempotencyKeyRepository r2dbcRepository;

    @Override
    public Mono<Boolean> exists(String idempotencyKey) {
        log.debug("Checking if idempotency key exists: {}", idempotencyKey);
        return r2dbcRepository.existsByIdempotencyKey(idempotencyKey);
    }

    @Override
    public Mono<Void> save(String idempotencyKey, String paymentOrderId) {
        log.debug("Saving idempotency key: {} -> {}", idempotencyKey, paymentOrderId);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(EXPIRATION_HOURS, ChronoUnit.HOURS);

        IdempotencyKeyEntity entity = IdempotencyKeyEntity.builder()
                .idempotencyKey(idempotencyKey)
                .paymentOrderId(paymentOrderId)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        return r2dbcRepository.save(entity)
                .doOnSuccess(saved -> log.debug("Idempotency key saved: {}", idempotencyKey))
                .then();
    }

    @Override
    public Mono<String> findPaymentOrderId(String idempotencyKey) {
        log.debug("Finding payment order ID for idempotency key: {}", idempotencyKey);

        return r2dbcRepository.findByIdempotencyKey(idempotencyKey)
                .map(IdempotencyKeyEntity::getPaymentOrderId);
    }
}
