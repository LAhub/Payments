package com.ec.bank.payments.application.service;

import com.ec.bank.payments.domain.exception.InvalidPaymentOrderException;
import com.ec.bank.payments.domain.exception.PaymentOrderNotFoundException;
import com.ec.bank.payments.domain.model.*;
import com.ec.bank.payments.application.port.input.InitiatePaymentOrderUseCase.InitiatePaymentOrderCommand;
import com.ec.bank.payments.domain.port.output.PaymentOrderRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentOrderService.
 */
@Disabled("Integration tests are disabled by default")
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentOrderService Tests")
class PaymentOrderServiceTest {

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private IdempotencyService idempotencyService;

    private SimpleMeterRegistry meterRegistry;
    private PaymentOrderService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new PaymentOrderService(
                paymentOrderRepository,
                idempotencyService,
                meterRegistry
        );
    }

    @Nested
    @DisplayName("Initiate Payment Order Tests")
    class InitiateTests {

        @Test
        @DisplayName("Should successfully initiate payment order")
        void shouldInitiatePaymentOrder() {
            // Given
            InitiatePaymentOrderCommand command = createValidCommand();

            PaymentOrder expectedOrder = PaymentOrder.create(
                    command.paymentOrderReference(),
                    IBAN.of(command.debtorAccount()),
                    IBAN.of(command.creditorAccount()),
                    Amount.of(command.amount(), command.currency()),
                    command.remittanceInformation(),
                    command.requestedExecutionDateTime()
            );

            when(idempotencyService.checkIdempotency(command.idempotencyKey()))
                    .thenReturn(Mono.empty());
            when(paymentOrderRepository.save(any(PaymentOrder.class)))
                    .thenReturn(Mono.just(expectedOrder));
            when(idempotencyService.saveIdempotencyKey(anyString(), anyString()))
                    .thenReturn(Mono.empty());

            // When/Then
            StepVerifier.create(service.initiate(command))
                    .assertNext(order -> {
                        assertThat(order).isNotNull();
                        assertThat(order.getPaymentOrderReference()).isEqualTo(command.paymentOrderReference());
                        assertThat(order.getStatus()).isEqualTo(PaymentStatus.PENDING);
                    })
                    .verifyComplete();

            verify(idempotencyService).checkIdempotency(command.idempotencyKey());
            verify(paymentOrderRepository).save(any(PaymentOrder.class));
            verify(idempotencyService).saveIdempotencyKey(anyString(), anyString());
        }

        @Test
        @DisplayName("Should fail with null command")
        void shouldFailWithNullCommand() {
            // When/Then
            StepVerifier.create(service.initiate(null))
                    .expectError(InvalidPaymentOrderException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should fail with missing required fields")
        void shouldFailWithMissingFields() {
            // Given
            InitiatePaymentOrderCommand command = new InitiatePaymentOrderCommand(
                    null, // missing reference
                    "ES7921000813610123456789",
                    "ES1420805801101234567891",
                    1500.00,
                    "EUR",
                    "Test",
                    LocalDate.now().plusDays(1),
                    "key-123"
            );

            // When/Then
            StepVerifier.create(service.initiate(command))
                    .expectError(InvalidPaymentOrderException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should fail with invalid amount")
        void shouldFailWithInvalidAmount() {
            // Given
            InitiatePaymentOrderCommand command = createValidCommand();
            InitiatePaymentOrderCommand invalidCommand = new InitiatePaymentOrderCommand(
                    command.paymentOrderReference(),
                    command.debtorAccount(),
                    command.creditorAccount(),
                    -100.00, // negative amount
                    command.currency(),
                    command.remittanceInformation(),
                    command.requestedExecutionDateTime(),
                    command.idempotencyKey()
            );

            // When/Then
            StepVerifier.create(service.initiate(invalidCommand))
                    .expectError(InvalidPaymentOrderException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should fail with invalid IBAN")
        void shouldFailWithInvalidIban() {
            // Given
            InitiatePaymentOrderCommand command = new InitiatePaymentOrderCommand(
                    "REF-001",
                    "INVALID-IBAN",
                    "ES1420805801101234567891",
                    1500.00,
                    "EUR",
                    "Test",
                    LocalDate.now().plusDays(1),
                    "key-123"
            );

            // When/Then
            StepVerifier.create(service.initiate(command))
                    .expectError(InvalidPaymentOrderException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Retrieve Payment Order Tests")
    class RetrieveTests {

        @Test
        @DisplayName("Should successfully retrieve payment order")
        void shouldRetrievePaymentOrder() {
            // Given
            PaymentOrderId orderId = PaymentOrderId.of("PO-001");
            PaymentOrder expectedOrder = createTestPaymentOrder(orderId);

            when(paymentOrderRepository.findById(orderId))
                    .thenReturn(Mono.just(expectedOrder));

            // When/Then
            StepVerifier.create(service.retrieve(orderId))
                    .assertNext(order -> {
                        assertThat(order).isNotNull();
                        assertThat(order.getPaymentOrderReference()).isEqualTo("REF-2025-001");
                        assertThat(order.getStatus()).isEqualTo(PaymentStatus.PENDING);
                    })
                    .verifyComplete();

            verify(paymentOrderRepository).findById(orderId);
        }

        @Test
        @DisplayName("Should fail when payment order not found")
        void shouldFailWhenNotFound() {
            // Given
            PaymentOrderId orderId = PaymentOrderId.of("PO-999");

            when(paymentOrderRepository.findById(orderId))
                    .thenReturn(Mono.empty());

            // When/Then
            StepVerifier.create(service.retrieve(orderId))
                    .expectError(PaymentOrderNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Retrieve Status Tests")
    class RetrieveStatusTests {

        @Test
        @DisplayName("Should successfully retrieve payment order status")
        void shouldRetrieveStatus() {
            // Given
            PaymentOrderId orderId = PaymentOrderId.of("PO-001");
            PaymentOrder order = createTestPaymentOrder(orderId);

            when(paymentOrderRepository.findById(orderId))
                    .thenReturn(Mono.just(order));

            // When/Then
            StepVerifier.create(service.retrieveStatus(orderId))
                    .assertNext(status -> {
                        assertThat(status).isNotNull();
                        assertThat(status.paymentOrderId()).isEqualTo(orderId);
                        assertThat(status.status()).isEqualTo(PaymentStatus.PENDING);
                    })
                    .verifyComplete();
        }
    }

    // Helper methods

    private InitiatePaymentOrderCommand createValidCommand() {
        return new InitiatePaymentOrderCommand(
                "REF-2025-001",
                "ES7921000813610123456789",
                "ES1420805801101234567891",
                1500.00,
                "EUR",
                "Invoice payment",
                LocalDate.now().plusDays(1),
                "idempotency-key-123"
        );
    }

    private PaymentOrder createTestPaymentOrder(PaymentOrderId orderId) {
        PaymentOrder order = PaymentOrder.create(
                "REF-2025-001",
                IBAN.of("ES7921000813610123456789"),
                IBAN.of("ES1420805801101234567891"),
                Amount.of(1500.00, "EUR"),
                "Invoice payment",
                LocalDate.now().plusDays(1)
        );

        // Usar reflection para setear el ID específico
        return order.toBuilder()
                .paymentOrderId(orderId)
                .build();
    }
}