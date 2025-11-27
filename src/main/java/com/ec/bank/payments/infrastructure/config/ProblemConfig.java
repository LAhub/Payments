package com.ec.bank.payments.infrastructure.config;


import com.ec.bank.payments.domain.exception.DuplicatePaymentOrderException;
import com.ec.bank.payments.domain.exception.InvalidPaymentOrderException;
import com.ec.bank.payments.domain.exception.PaymentOrderNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.zalando.problem.spring.webflux.advice.ProblemHandling;

import java.net.URI;
import java.time.Instant;

/**
 * Global exception handler using RFC 7807 Problem Details.
 * Converts domain exceptions to standardized HTTP problem responses.
 */
@Slf4j
@Configuration
public class ProblemConfig implements ProblemHandling {

    /**
     * Custom exception handler for PaymentOrderNotFoundException.
     */
    @Bean
    public org.springframework.web.reactive.function.server.HandlerFilterFunction<ServerResponse, ServerResponse>
    paymentOrderNotFoundHandler() {
        return (request, next) -> next.handle(request)
                .onErrorResume(PaymentOrderNotFoundException.class, ex -> {
                    log.error("Payment order not found: {}", ex.getPaymentOrderId().value());

                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                            HttpStatus.NOT_FOUND,
                            ex.getMessage()
                    );
                    problem.setTitle("Payment Order Not Found");
                    problem.setType(URI.create("https://api.bank.com/problems/payment-not-found"));
                    problem.setProperty("paymentOrderId", ex.getPaymentOrderId().value());
                    problem.setProperty("timestamp", Instant.now());

                    return ServerResponse.status(HttpStatus.NOT_FOUND)
                            .bodyValue(problem);
                });
    }

    /**
     * Custom exception handler for DuplicatePaymentOrderException.
     */
    @Bean
    public org.springframework.web.reactive.function.server.HandlerFilterFunction<ServerResponse, ServerResponse>
    duplicatePaymentOrderHandler() {
        return (request, next) -> next.handle(request)
                .onErrorResume(DuplicatePaymentOrderException.class, ex -> {
                    log.warn("Duplicate payment order: idempotency={}, existing={}",
                            ex.getIdempotencyKey(), ex.getExistingPaymentOrderId());

                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                            HttpStatus.CONFLICT,
                            ex.getMessage()
                    );
                    problem.setTitle("Duplicate Payment Order");
                    problem.setType(URI.create("https://api.bank.com/problems/duplicate-payment"));
                    problem.setProperty("idempotencyKey", ex.getIdempotencyKey());
                    problem.setProperty("existingPaymentOrderId", ex.getExistingPaymentOrderId());
                    problem.setProperty("timestamp", Instant.now());

                    return ServerResponse.status(HttpStatus.CONFLICT)
                            .bodyValue(problem);
                });
    }

    /**
     * Custom exception handler for InvalidPaymentOrderException.
     */
    @Bean
    public org.springframework.web.reactive.function.server.HandlerFilterFunction<ServerResponse, ServerResponse>
    invalidPaymentOrderHandler() {
        return (request, next) -> next.handle(request)
                .onErrorResume(InvalidPaymentOrderException.class, ex -> {
                    log.error("Invalid payment order: {}", ex.getMessage());

                    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                            HttpStatus.BAD_REQUEST,
                            ex.getMessage()
                    );
                    problem.setTitle("Invalid Payment Order");
                    problem.setType(URI.create("https://api.bank.com/problems/invalid-payment"));
                    problem.setProperty("timestamp", Instant.now());

                    return ServerResponse.status(HttpStatus.BAD_REQUEST)
                            .bodyValue(problem);
                });
    }
}
