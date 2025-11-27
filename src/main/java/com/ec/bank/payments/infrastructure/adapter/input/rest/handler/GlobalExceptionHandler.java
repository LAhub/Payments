package com.ec.bank.payments.infrastructure.adapter.input.rest.handler;

import com.ec.bank.payments.domain.exception.DuplicatePaymentOrderException;
import com.ec.bank.payments.domain.exception.InvalidPaymentOrderException;
import com.ec.bank.payments.domain.exception.PaymentOrderNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers.
 * Implements RFC 7807 Problem Details for API errors.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles PaymentOrderNotFoundException (404).
     */
    @ExceptionHandler(PaymentOrderNotFoundException.class)
    public Mono<ResponseEntity<ProblemDetail>> handlePaymentOrderNotFound(PaymentOrderNotFoundException ex) {
        log.error("Payment order not found: {}", ex.getPaymentOrderId().value());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Payment Order Not Found");
        problem.setType(URI.create("https://api.bank.com/problems/payment-not-found"));
        problem.setProperty("paymentOrderId", ex.getPaymentOrderId().value());
        problem.setProperty("timestamp", Instant.now());

        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem));
    }

    /**
     * Handles DuplicatePaymentOrderException (409 Conflict).
     */
    @ExceptionHandler(DuplicatePaymentOrderException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleDuplicatePaymentOrder(DuplicatePaymentOrderException ex) {
        log.warn("Duplicate payment order detected: idempotency={}", ex.getIdempotencyKey());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Duplicate Payment Order");
        problem.setType(URI.create("https://api.bank.com/problems/duplicate-payment"));
        problem.setProperty("idempotencyKey", ex.getIdempotencyKey());
        problem.setProperty("existingPaymentOrderId", ex.getExistingPaymentOrderId());
        problem.setProperty("timestamp", Instant.now());

        return Mono.just(ResponseEntity
                .status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem));
    }

    /**
     * Handles InvalidPaymentOrderException (400 Bad Request).
     */
    @ExceptionHandler(InvalidPaymentOrderException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleInvalidPaymentOrder(InvalidPaymentOrderException ex) {
        log.error("Invalid payment order: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Invalid Payment Order");
        problem.setType(URI.create("https://api.bank.com/problems/invalid-payment"));
        problem.setProperty("timestamp", Instant.now());

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem));
    }


    /**
     * Handles IllegalArgumentException (400 Bad Request).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Invalid Request");
        problem.setType(URI.create("https://api.bank.com/problems/invalid-request"));
        problem.setProperty("timestamp", Instant.now());

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem));
    }

    /**
     * Handles generic exceptions (500 Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ProblemDetail>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://api.bank.com/problems/internal-error"));
        problem.setProperty("timestamp", Instant.now());

        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem));
    }
}
