package com.ec.bank.payments.application.port.input;


import com.ec.bank.payments.domain.model.PaymentOrder;
import com.ec.bank.payments.domain.model.PaymentOrderId;
import reactor.core.publisher.Mono;

/**
 * Use Case: Retrieve payment order details.
 */
public interface RetrievePaymentOrderUseCase {

    /**
     * Retrieves complete payment order information.
     *
     * @param paymentOrderId The unique identifier
     * @return Mono emitting the PaymentOrder or empty if not found
     */
    Mono<PaymentOrder> retrieve(PaymentOrderId paymentOrderId);
}
