package com.ec.bank.payments.infrastructure.adapter.input.rest.mapper;

import com.ec.bank.payments.domain.model.Amount;
import com.ec.bank.payments.domain.model.PaymentOrder;
import com.ec.bank.payments.application.port.input.InitiatePaymentOrderUseCase.InitiatePaymentOrderCommand;
import com.ec.bank.payments.application.port.input.RetrievePaymentOrderStatusUseCase.PaymentOrderStatusInfo;
import com.ec.bank.payments.infrastructure.adapter.input.rest.dto.*;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Mapper entre DTOs REST (generados por OpenAPI) y modelos de Dominio.
 * Parte del adaptador de entrada en arquitectura hexagonal.
 */
@Component
public class PaymentOrderMapper {

    /**
     * Convierte InitiatePaymentOrderRequestDto a InitiatePaymentOrderCommand.
     */
    public InitiatePaymentOrderCommand toCommand(
            InitiatePaymentOrderRequestDto request,
            String idempotencyKey) {

        return new InitiatePaymentOrderCommand(
                request.getPaymentOrderReference(),
                request.getDebtorAccount(),
                request.getCreditorAccount(),
                request.getInstructedAmount().getAmount(),
                request.getInstructedAmount().getCurrency(),
                request.getRemittanceInformation(),
                request.getRequestedExecutionDateTime(),
                idempotencyKey
        );
    }

    /**
     * Convierte PaymentOrder de dominio a InitiatePaymentOrderResponseDto.
     */
    public InitiatePaymentOrderResponseDto toInitiateResponse(PaymentOrder paymentOrder) {
        InitiatePaymentOrderResponseDto response = new InitiatePaymentOrderResponseDto();
        response.setPaymentOrderId(paymentOrder.getPaymentOrderId().value());
        response.setPaymentOrderStatus(toPaymentStatusDto(paymentOrder.getStatus()));
        response.setCreatedAt(toOffsetDateTime(paymentOrder.getCreatedAt()));
        return response;
    }

    /**
     * Convierte PaymentOrder de dominio a PaymentOrderDetailsDto.
     */
    public PaymentOrderDetailsDto toPaymentOrderDetails(PaymentOrder paymentOrder) {
        PaymentOrderDetailsDto details = new PaymentOrderDetailsDto();
        details.setPaymentOrderId(paymentOrder.getPaymentOrderId().value());
        details.setPaymentOrderReference(paymentOrder.getPaymentOrderReference());
        details.setDebtorAccount(paymentOrder.getDebtorAccount().value());
        details.setCreditorAccount(paymentOrder.getCreditorAccount().value());
        details.setInstructedAmount(toAmountDto(paymentOrder.getInstructedAmount()));
        details.setRemittanceInformation(paymentOrder.getRemittanceInformation());
        details.setRequestedExecutionDateTime(paymentOrder.getRequestedExecutionDateTime());
        details.setPaymentOrderStatus(toPaymentStatusDto(paymentOrder.getStatus()));
        details.setCreatedAt(toOffsetDateTime(paymentOrder.getCreatedAt()));
        details.setLastUpdatedAt(toOffsetDateTime(paymentOrder.getLastUpdatedAt()));
        return details;
    }

    /**
     * Convierte PaymentOrderStatusInfo a PaymentOrderStatusDto.
     */
    public PaymentOrderStatusDto toPaymentOrderStatus(PaymentOrderStatusInfo statusInfo) {
        PaymentOrderStatusDto status = new PaymentOrderStatusDto();
        status.setPaymentOrderId(statusInfo.paymentOrderId().value());
        status.setPaymentOrderStatus(toPaymentStatusDto(statusInfo.status()));
        status.setLastUpdatedAt(toOffsetDateTime(statusInfo.lastUpdatedAt()));
        return status;
    }

    /**
     * Convierte Amount de dominio a AmountDto.
     */
    private AmountDto toAmountDto(Amount domainAmount) {
        AmountDto dto = new AmountDto();
        dto.setAmount(domainAmount.value().doubleValue());
        dto.setCurrency(domainAmount.currencyCode());
        return dto;
    }

    /**
     * Convierte PaymentStatus de dominio a PaymentStatusDto.
     */
    private PaymentStatusDto toPaymentStatusDto(
            com.ec.bank.payments.domain.model.PaymentStatus domainStatus) {
        return PaymentStatusDto.fromValue(domainStatus.name());
    }

    /**
     * Convierte Instant a OffsetDateTime en UTC.
     */
    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}