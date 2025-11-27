package com.ec.bank.payments.domain.exception;


import com.ec.bank.payments.domain.model.PaymentOrderId;

/**
 * Exception thrown when a payment order is not found.
 */
public class PaymentOrderNotFoundException extends RuntimeException {

    private final PaymentOrderId paymentOrderId;

    public PaymentOrderNotFoundException(PaymentOrderId paymentOrderId) {
        super(String.format("Payment order not found: %s", paymentOrderId.value()));
        this.paymentOrderId = paymentOrderId;
    }

    public PaymentOrderId getPaymentOrderId() {
        return paymentOrderId;
    }
}
