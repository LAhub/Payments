
package com.ec.bank.payments.infrastructure.adapter.input.rest;

import com.ec.bank.payments.domain.model.PaymentOrderId;
import com.ec.bank.payments.application.port.input.InitiatePaymentOrderUseCase;
import com.ec.bank.payments.application.port.input.RetrievePaymentOrderStatusUseCase;
import com.ec.bank.payments.application.port.input.RetrievePaymentOrderUseCase;
import com.ec.bank.payments.infrastructure.adapter.input.rest.api.PaymentOrdersApi;
import com.ec.bank.payments.infrastructure.adapter.input.rest.dto.InitiatePaymentOrderRequestDto;
import com.ec.bank.payments.infrastructure.adapter.input.rest.dto.InitiatePaymentOrderResponseDto;
import com.ec.bank.payments.infrastructure.adapter.input.rest.dto.PaymentOrderDetailsDto;
import com.ec.bank.payments.infrastructure.adapter.input.rest.dto.PaymentOrderStatusDto;
import com.ec.bank.payments.infrastructure.adapter.input.rest.mapper.PaymentOrderMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST Controller que implementa PaymentOrdersApi generada por OpenAPI.
 * Contract-First: La interfaz define el contrato, el controller lo implementa.
 * Adaptador de entrada en arquitectura hexagonal.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentOrderController implements PaymentOrdersApi {

    private final InitiatePaymentOrderUseCase initiatePaymentOrderUseCase;
    private final RetrievePaymentOrderUseCase retrievePaymentOrderUseCase;
    private final RetrievePaymentOrderStatusUseCase retrievePaymentOrderStatusUseCase;
    private final PaymentOrderMapper mapper;

    /**
     * POST /payment-initiation/payment-orders
     * Inicia una nueva orden de pago.
     */
    @Override
    @Timed(value = "api.payment.initiate", description = "Time taken to initiate payment")
    public Mono<ResponseEntity<InitiatePaymentOrderResponseDto>> initiatePaymentOrder(
            Mono<InitiatePaymentOrderRequestDto> initiatePaymentOrderRequestDto,
            UUID idempotencyKey,
            ServerWebExchange exchange) {

        log.info("Received initiate payment order request with idempotency key: {}", idempotencyKey);

        String idempotencyKeyStr = idempotencyKey != null ? idempotencyKey.toString() : null;

        return initiatePaymentOrderRequestDto
                .doOnNext(req -> log.debug("Request: ref={}, debtor={}, amount={}",
                        req.getPaymentOrderReference(),
                        req.getDebtorAccount(),
                        req.getInstructedAmount().getAmount()))
                .map(req -> mapper.toCommand(req, idempotencyKeyStr))
                .flatMap(initiatePaymentOrderUseCase::initiate)
                .map(mapper::toInitiateResponse)
                .map(response -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(response))
                .doOnSuccess(res -> log.info("Payment order created: {}",
                        res.getBody().getPaymentOrderId()))
                .doOnError(e -> log.error("Failed to initiate payment", e));
    }

    /**
     * GET /payment-initiation/payment-orders/{paymentOrderId}
     * Recupera los detalles de una orden de pago.
     */
    @Override
    public Mono<ResponseEntity<PaymentOrderDetailsDto>> retrievePaymentOrder(
            String paymentOrderId,
            ServerWebExchange exchange) {

        log.info("Retrieving payment order: {}", paymentOrderId);

        return retrievePaymentOrderUseCase
                .retrieve(PaymentOrderId.of(paymentOrderId))
                .map(mapper::toPaymentOrderDetails)
                .map(ResponseEntity::ok)
                .doOnSuccess(res -> log.info("Payment order retrieved: {}", paymentOrderId));
    }

    /**
     * GET /payment-initiation/payment-orders/{paymentOrderId}/status
     * Recupera el estado de una orden de pago.
     */
    @Override
    public Mono<ResponseEntity<PaymentOrderStatusDto>> retrievePaymentOrderStatus(
            String paymentOrderId,
            ServerWebExchange exchange) {

        log.info("Retrieving payment order status: {}", paymentOrderId);

        return retrievePaymentOrderStatusUseCase
                .retrieveStatus(PaymentOrderId.of(paymentOrderId))
                .map(mapper::toPaymentOrderStatus)
                .map(ResponseEntity::ok)
                .doOnSuccess(res -> log.info("Status retrieved: {} -> {}",
                        paymentOrderId,
                        res.getBody().getPaymentOrderStatus()));
    }
}