package com.ec.bank.payments.infrastructure.adapter.output.persistence.repository;

import com.ec.bank.payments.infrastructure.adapter.output.persistence.entity.PaymentOrderEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * R2DBC reactive repository for PaymentOrderEntity.
 */
@Repository
public interface R2dbcPaymentOrderRepository extends R2dbcRepository<PaymentOrderEntity, Long> {

    /**
     * Finds a payment order by its unique payment_order_id.
     */
    @Query("SELECT * FROM payment_orders WHERE payment_order_id = :paymentOrderId")
    Mono<PaymentOrderEntity> findByPaymentOrderId(String paymentOrderId);

    /**
     * Finds a payment order by its external reference.
     */
    @Query("SELECT * FROM payment_orders WHERE payment_order_reference = :reference")
    Mono<PaymentOrderEntity> findByPaymentOrderReference(String reference);

    /**
     * Checks if a payment order exists by its payment_order_id.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM payment_orders WHERE payment_order_id = :paymentOrderId)")
    Mono<Boolean> existsByPaymentOrderId(String paymentOrderId);
}
