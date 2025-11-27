package com.ec.bank.payments.application.port.input;


import com.ec.bank.payments.domain.model.PaymentOrderId;
import com.ec.bank.payments.domain.model.PaymentStatus;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Use Case: Retrieve payment order status.
 */
public interface RetrievePaymentOrderStatusUseCase {

    /**
     * Retrieves only the status information of a payment order.
     *
     * @param paymentOrderId The unique identifier
     * @return Mono emitting the status information
     */
    Mono<PaymentOrderStatusInfo> retrieveStatus(PaymentOrderId paymentOrderId);

    /**
     * Status information DTO.
     */
    record PaymentOrderStatusInfo(
            PaymentOrderId paymentOrderId,
            PaymentStatus status,
            Instant lastUpdatedAt
    ) {}
}
