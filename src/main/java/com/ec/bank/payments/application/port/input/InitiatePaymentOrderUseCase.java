package com.ec.bank.payments.application.port.input;



import com.ec.bank.payments.domain.model.PaymentOrder;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Use Case: Initiate a new payment order.
 * Driving port (exposed to external actors).
 */
public interface InitiatePaymentOrderUseCase {

    /**
     * Initiates a new payment order with idempotency support.
     *
     * @param command Contains all required data to create a payment order
     * @return Mono emitting the created PaymentOrder
     */
    Mono<PaymentOrder> initiate(InitiatePaymentOrderCommand command);

    /**
     * Command object encapsulating payment order creation data.
     */
    record InitiatePaymentOrderCommand(
            String paymentOrderReference,
            String debtorAccount,
            String creditorAccount,
            Double amount,
            String currency,
            String remittanceInformation,
            LocalDate requestedExecutionDateTime,
            String idempotencyKey
    ) {}
}
