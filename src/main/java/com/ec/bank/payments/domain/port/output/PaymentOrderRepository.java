package com.ec.bank.payments.domain.port.output;


import com.ec.bank.payments.domain.model.PaymentOrder;
import com.ec.bank.payments.domain.model.PaymentOrderId;
import reactor.core.publisher.Mono;

/**
 * Output port for payment order persistence.
 * To be implemented by infrastructure layer (R2DBC adapter).
 */
public interface PaymentOrderRepository {

    /**
     * Saves a payment order.
     *
     * @param paymentOrder The order to save
     * @return Mono emitting the saved order
     */
    Mono<PaymentOrder> save(PaymentOrder paymentOrder);

    /**
     * Finds a payment order by its ID.
     *
     * @param paymentOrderId The unique identifier
     * @return Mono emitting the order or empty if not found
     */
    Mono<PaymentOrder> findById(PaymentOrderId paymentOrderId);

    /**
     * Finds a payment order by reference.
     *
     * @param reference The external reference
     * @return Mono emitting the order or empty if not found
     */
    Mono<PaymentOrder> findByReference(String reference);

    /**
     * Checks if a payment order exists.
     *
     * @param paymentOrderId The unique identifier
     * @return Mono emitting true if exists, false otherwise
     */
    Mono<Boolean> existsById(PaymentOrderId paymentOrderId);
}
