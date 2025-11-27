package com.ec.bank.payments.infrastructure.adapter.output.persistence.repository;

import com.ec.bank.payments.infrastructure.adapter.output.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * R2DBC reactive repository for IdempotencyKeyEntity.
 */
@Repository
public interface R2dbcIdempotencyKeyRepository extends R2dbcRepository<IdempotencyKeyEntity, Long> {

    /**
     * Finds an idempotency key entry.
     */
    @Query("SELECT * FROM idempotency_keys WHERE idempotency_key = :idempotencyKey")
    Mono<IdempotencyKeyEntity> findByIdempotencyKey(String idempotencyKey);

    /**
     * Checks if an idempotency key exists and is not expired.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM idempotency_keys WHERE idempotency_key = :idempotencyKey AND expires_at > NOW())")
    Mono<Boolean> existsByIdempotencyKey(String idempotencyKey);
}
