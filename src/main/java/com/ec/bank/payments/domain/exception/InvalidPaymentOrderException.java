package com.ec.bank.payments.domain.exception;

/**
 * Exception thrown for invalid payment order operations.
 */
public class InvalidPaymentOrderException extends RuntimeException {

    public InvalidPaymentOrderException(String message) {
        super(message);
    }

    public InvalidPaymentOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
