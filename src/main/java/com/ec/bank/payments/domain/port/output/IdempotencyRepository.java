package com.ec.bank.payments.domain.port.output;


import reactor.core.publisher.Mono;

/**
 * Output port for idempotency management.
 * Ensures duplicate requests are not processed twice.
 */
public interface IdempotencyRepository {

    /**
     * Checks if an idempotency key already exists.
     *
     * @param idempotencyKey The unique key
     * @return Mono emitting true if key exists
     */
    Mono<Boolean> exists(String idempotencyKey);

    /**
     * Saves an idempotency key with associated payment order ID.
     *
     * @param idempotencyKey The unique key
     * @param paymentOrderId The payment order ID
     * @return Mono emitting void when complete
     */
    Mono<Void> save(String idempotencyKey, String paymentOrderId);

    /**
     * Retrieves the payment order ID associated with an idempotency key.
     *
     * @param idempotencyKey The unique key
     * @return Mono emitting the payment order ID or empty
     */
    Mono<String> findPaymentOrderId(String idempotencyKey);
}
