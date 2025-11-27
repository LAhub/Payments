package com.ec.bank.payments.infrastructure.adapter.output.persistence.adapter;

import com.ec.bank.payments.domain.model.*;
import com.ec.bank.payments.domain.port.output.PaymentOrderRepository;
import com.ec.bank.payments.infrastructure.adapter.output.persistence.entity.PaymentOrderEntity;
import com.ec.bank.payments.infrastructure.adapter.output.persistence.repository.R2dbcPaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Adapter implementing PaymentOrderRepository port using R2DBC.
 * Converts between domain models and persistence entities.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOrderRepositoryAdapter implements PaymentOrderRepository {

    private final R2dbcPaymentOrderRepository r2dbcRepository;

    @Override
    public Mono<PaymentOrder> save(PaymentOrder paymentOrder) {
        log.debug("Saving payment order: {}", paymentOrder.getPaymentOrderId().value());

        PaymentOrderEntity entity = toEntity(paymentOrder);

        return r2dbcRepository.save(entity)
                .map(this::toDomain)
                .doOnSuccess(saved -> log.debug("Payment order saved: {}",
                        saved.getPaymentOrderId().value()))
                .doOnError(e -> log.error("Failed to save payment order", e));
    }

    @Override
    public Mono<PaymentOrder> findById(PaymentOrderId paymentOrderId) {
        log.debug("Finding payment order by ID: {}", paymentOrderId.value());

        return r2dbcRepository.findByPaymentOrderId(paymentOrderId.value())
                .map(this::toDomain)
                .doOnSuccess(found -> {
                    if (found != null) {
                        log.debug("Payment order found: {}", paymentOrderId.value());
                    } else {
                        log.debug("Payment order not found: {}", paymentOrderId.value());
                    }
                });
    }

    @Override
    public Mono<PaymentOrder> findByReference(String reference) {
        log.debug("Finding payment order by reference: {}", reference);

        return r2dbcRepository.findByPaymentOrderReference(reference)
                .map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsById(PaymentOrderId paymentOrderId) {
        return r2dbcRepository.existsByPaymentOrderId(paymentOrderId.value());
    }

    // ==================== Mapping Methods ====================

    /**
     * Converts domain PaymentOrder to persistence entity.
     */
    private PaymentOrderEntity toEntity(PaymentOrder domain) {
        return PaymentOrderEntity.builder()
                .paymentOrderId(domain.getPaymentOrderId().value())
                .paymentOrderReference(domain.getPaymentOrderReference())
                .debtorAccount(domain.getDebtorAccount().value())
                .creditorAccount(domain.getCreditorAccount().value())
                .amount(domain.getInstructedAmount().value())
                .currency(domain.getInstructedAmount().currencyCode())
                .remittanceInformation(domain.getRemittanceInformation())
                .requestedExecutionDate(domain.getRequestedExecutionDateTime())
                .status(domain.getStatus().name())
                .createdAt(domain.getCreatedAt())
                .lastUpdatedAt(domain.getLastUpdatedAt())
                .build();
    }

    /**
     * Converts persistence entity to domain PaymentOrder.
     */
    private PaymentOrder toDomain(PaymentOrderEntity entity) {
        return PaymentOrder.builder()
                .paymentOrderId(PaymentOrderId.of(entity.getPaymentOrderId()))
                .paymentOrderReference(entity.getPaymentOrderReference())
                .debtorAccount(IBAN.of(entity.getDebtorAccount()))
                .creditorAccount(IBAN.of(entity.getCreditorAccount()))
                .instructedAmount(Amount.of(entity.getAmount(), entity.getCurrency()))
                .remittanceInformation(entity.getRemittanceInformation())
                .requestedExecutionDateTime(entity.getRequestedExecutionDate())
                .status(PaymentStatus.valueOf(entity.getStatus()))
                .createdAt(entity.getCreatedAt())
                .lastUpdatedAt(entity.getLastUpdatedAt())
                .build();
    }
}
