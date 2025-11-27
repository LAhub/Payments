package com.ec.bank.payments.application.service;

import com.ec.bank.payments.domain.exception.InvalidPaymentOrderException;
import com.ec.bank.payments.domain.exception.PaymentOrderNotFoundException;
import com.ec.bank.payments.domain.model.Amount;
import com.ec.bank.payments.domain.model.IBAN;
import com.ec.bank.payments.domain.model.PaymentOrder;
import com.ec.bank.payments.domain.model.PaymentOrderId;
import com.ec.bank.payments.application.port.input.InitiatePaymentOrderUseCase;
import com.ec.bank.payments.application.port.input.RetrievePaymentOrderStatusUseCase;
import com.ec.bank.payments.application.port.input.RetrievePaymentOrderUseCase;
import com.ec.bank.payments.domain.port.output.PaymentOrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
@Service
public class PaymentOrderService implements
        InitiatePaymentOrderUseCase,
        RetrievePaymentOrderUseCase,
        RetrievePaymentOrderStatusUseCase {

    private final PaymentOrderRepository paymentOrderRepository;
    private final IdempotencyService idempotencyService;

    private final Counter paymentInitiatedCounter;
    private final Counter paymentRetrievedCounter;
    private final Counter paymentNotFoundCounter;
    private final Timer paymentInitiationTimer;

    public PaymentOrderService(
            PaymentOrderRepository paymentOrderRepository,
            IdempotencyService idempotencyService,
            MeterRegistry meterRegistry) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.idempotencyService = idempotencyService;

        this.paymentInitiatedCounter = Counter.builder("payment.order.initiated")
                .description("Number of payment orders initiated")
                .register(meterRegistry);

        this.paymentRetrievedCounter = Counter.builder("payment.order.retrieved")
                .description("Number of payment orders retrieved")
                .register(meterRegistry);

        this.paymentNotFoundCounter = Counter.builder("payment.order.not.found")
                .description("Number of payment order not found errors")
                .register(meterRegistry);

        this.paymentInitiationTimer = Timer.builder("payment.order.initiation.duration")
                .description("Time taken to initiate a payment order")
                .register(meterRegistry);
    }


    @Override
    @Transactional
    public Mono<PaymentOrder> initiate(InitiatePaymentOrderCommand command) {
        log.info("Initiating payment order with reference: {}, idempotency key: {}",
                command != null ? command.paymentOrderReference() : "null",
                command != null ? command.idempotencyKey() : "null");

        return validateCommand(command)
                .then(Mono.defer(() -> {
                    if (command != null && command.idempotencyKey() != null) {
                        return idempotencyService.checkIdempotency(command.idempotencyKey());
                    }
                    return Mono.empty();
                }))
                .then(Mono.defer(() -> createPaymentOrder(command)))
                .flatMap(paymentOrderRepository::save)
                .flatMap(savedOrder -> saveIdempotencyAndReturn(command, savedOrder))
                .doOnSuccess(order -> {
                    paymentInitiatedCounter.increment();
                    log.info("Payment order initiated successfully: {}",
                            order.getPaymentOrderId().value());
                })
                .doOnError(e -> {
                    log.error("Failed to initiate payment order - reference: {}, idempotency key: {}, error type: {}",
                            command != null ? command.paymentOrderReference() : "null",
                            command != null ? command.idempotencyKey() : "null",
                            e.getClass().getSimpleName(),
                            e);
                });
    }

    
    public Mono<PaymentOrder> retrieve(PaymentOrderId paymentOrderId) {
        log.debug("Retrieving payment order: {}", paymentOrderId.value());

        return paymentOrderRepository.findById(paymentOrderId)
                .switchIfEmpty(Mono.defer(() -> {
                    paymentNotFoundCounter.increment();
                    log.warn("Payment order not found: {}", paymentOrderId.value());
                    return Mono.error(new PaymentOrderNotFoundException(paymentOrderId));
                }))
                .doOnSuccess(order -> {
                    paymentRetrievedCounter.increment();
                    log.debug("Payment order retrieved successfully: {}", paymentOrderId.value());
                });
    }

    @Override
    public Mono<PaymentOrderStatusInfo> retrieveStatus(PaymentOrderId paymentOrderId) {
        log.debug("Retrieving payment order status: {}", paymentOrderId.value());

        return retrieve(paymentOrderId)
                .map(order -> new PaymentOrderStatusInfo(
                        order.getPaymentOrderId(),
                        order.getStatus(),
                        order.getLastUpdatedAt()
                ))
                .doOnSuccess(status -> log.debug("Payment order status retrieved: {} -> {}",
                        paymentOrderId.value(), status.status()));
    }

    private Mono<Void> validateCommand(InitiatePaymentOrderCommand command) {
        try {
            Objects.requireNonNull(command, "Command cannot be null");
            Objects.requireNonNull(command.paymentOrderReference(),
                    "Payment order reference is required");
            Objects.requireNonNull(command.debtorAccount(),
                    "Debtor account is required");
            Objects.requireNonNull(command.creditorAccount(),
                    "Creditor account is required");
            Objects.requireNonNull(command.amount(),
                    "Amount is required");
            Objects.requireNonNull(command.currency(),
                    "Currency is required");
            Objects.requireNonNull(command.requestedExecutionDateTime(),
                    "Execution date is required");

            if (command.paymentOrderReference().isBlank()) {
                throw new InvalidPaymentOrderException("Payment order reference cannot be blank");
            }

            if (command.amount() <= 0) {
                throw new InvalidPaymentOrderException("Amount must be positive");
            }

            return Mono.empty();

        } catch (Exception e) {
            log.error("Command validation failed", e);
            return Mono.error(new InvalidPaymentOrderException(
                    "Invalid payment order command: " + e.getMessage(), e));
        }
    }

    private Mono<PaymentOrder> createPaymentOrder(InitiatePaymentOrderCommand command) {
        return Mono.fromCallable(() -> {
            IBAN debtorIban = IBAN.of(command.debtorAccount());
            IBAN creditorIban = IBAN.of(command.creditorAccount());
            Amount amount = Amount.of(
                    BigDecimal.valueOf(command.amount()),
                    command.currency()
            );

            return PaymentOrder.create(
                    command.paymentOrderReference(),
                    debtorIban,
                    creditorIban,
                    amount,
                    command.remittanceInformation(),
                    command.requestedExecutionDateTime()
            );
        }).onErrorMap(e -> {
            if (e instanceof InvalidPaymentOrderException) {
                return e;
            }
            log.error("Failed to create payment order domain object", e);
            return new InvalidPaymentOrderException(
                    "Failed to create payment order: " + e.getMessage(), e);
        });
    }

    private Mono<PaymentOrder> saveIdempotencyAndReturn(
            InitiatePaymentOrderCommand command,
            PaymentOrder savedOrder) {

        if (command == null || command.idempotencyKey() == null) {
            return Mono.just(savedOrder);
        }

        return idempotencyService.saveIdempotencyKey(
                        command.idempotencyKey(),
                        savedOrder.getPaymentOrderId().value()
                )
                .thenReturn(savedOrder);
    }
}