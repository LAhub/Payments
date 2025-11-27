package com.ec.bank.payments.infrastructure.adapter.input.rest;


import com.ec.bank.payments.infrastructure.adapter.input.rest.dto.*;
import org.junit.Ignore;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Payment Order API endpoints.
 */
@Disabled("Integration tests are disabled by default")
@DisplayName("Payment Order API Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentOrderControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private WebTestClient webTestClient;

    private static String createdPaymentOrderId;

    @Nested
    @DisplayName("POST /payment-initiation/payment-orders")
    class InitiatePaymentOrderTests {

        @Test
        @Order(1)
        @DisplayName("Should successfully create a payment order")
        void shouldCreatePaymentOrder() {
            // Given
            InitiatePaymentOrderRequestDto request = createValidRequest();
            String idempotencyKey = UUID.randomUUID().toString();

            // When/Then
            webTestClient.post()
                    .uri("/payment-initiation/payment-orders")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody(InitiatePaymentOrderResponseDto.class)
                    .value(response -> {
                        assertThat(response).isNotNull();
                        assertThat(response.getPaymentOrderId()).isNotNull();
                        assertThat(response.getPaymentOrderStatus()).isEqualTo(PaymentStatusDto.PENDING);
                        assertThat(response.getCreatedAt()).isNotNull();

                        // Store for later tests
                        createdPaymentOrderId = response.getPaymentOrderId();
                    });
        }

        @Test
        @Order(2)
        @DisplayName("Should return 409 Conflict for duplicate idempotency key")
        void shouldReturnConflictForDuplicateKey() {
            // Given
            InitiatePaymentOrderRequestDto request = createValidRequest();
            String idempotencyKey = "duplicate-key-" + UUID.randomUUID();

            // First request
            webTestClient.post()
                    .uri("/payment-initiation/payment-orders")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated();

            // Second request with same key
            webTestClient.post()
                    .uri("/payment-initiation/payment-orders")
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                    .expectHeader().contentType("application/problem+json")
                    .expectBody(ProblemDetail.class)
                    .value(problem -> {
                        assertThat(problem.getTitle()).isEqualTo("Duplicate Payment Order");
                        assertThat(problem.getStatus()).isEqualTo(409);
                    });
        }

        @Test
        @DisplayName("Should return 400 Bad Request for invalid IBAN")
        void shouldReturnBadRequestForInvalidIban() {
            // Given
            InitiatePaymentOrderRequestDto request = createValidRequest();
            request.setDebtorAccount("INVALID-IBAN");

            // When/Then
            webTestClient.post()
                    .uri("/payment-initiation/payment-orders")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentType("application/problem+json");
        }

        @Test
        @DisplayName("Should return 400 Bad Request for missing required fields")
        void shouldReturnBadRequestForMissingFields() {
            // Given
            InitiatePaymentOrderRequestDto request = new InitiatePaymentOrderRequestDto();
            // Missing all required fields

            // When/Then
            webTestClient.post()
                    .uri("/payment-initiation/payment-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("Should return 400 Bad Request for negative amount")
        void shouldReturnBadRequestForNegativeAmount() {
            // Given - raw JSON to bypass domain validation and test API validation
            String invalidRequestJson = """
                    {
                        "paymentOrderReference": "REF-TEST-NEG",
                        "debtorAccount": "ES7921000813610123456789",
                        "creditorAccount": "ES1420805801101234567891",
                        "instructedAmount": {
                            "value": -100.00,
                            "currency": "EUR"
                        },
                        "remittanceInformation": "Test payment with negative amount",
                        "requestedExecutionDateTime": "%s"
                    }
                    """.formatted(LocalDate.now().plusDays(1).toString());

            // When/Then
            webTestClient.post()
                    .uri("/payment-initiation/payment-orders")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(invalidRequestJson)
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Nested
        @DisplayName("GET /payment-initiation/payment-orders/{id}")
        class RetrievePaymentOrderTests {

            @Test
            @Order(3)
            @DisplayName("Should successfully retrieve payment order")
            void shouldRetrievePaymentOrder() {
                // Given - use payment order created in previous test
                assertThat(createdPaymentOrderId).isNotNull();

                // When/Then
                webTestClient.get()
                        .uri("/payment-initiation/payment-orders/{id}", createdPaymentOrderId)
                        .exchange()
                        .expectStatus().isOk()
                        .expectHeader().contentType(MediaType.APPLICATION_JSON)
                        .expectBody(PaymentOrderDetailsDto.class)
                        .value(details -> {
                            assertThat(details).isNotNull();
                            assertThat(details.getPaymentOrderId()).isEqualTo(createdPaymentOrderId);
                            assertThat(details.getPaymentOrderReference()).isNotNull();
                            assertThat(details.getDebtorAccount()).isNotNull();
                            assertThat(details.getCreditorAccount()).isNotNull();
                            assertThat(details.getInstructedAmount()).isNotNull();
                            assertThat(details.getPaymentOrderStatus()).isEqualTo(PaymentStatusDto.PENDING);
                        });
            }

            @Test
            @DisplayName("Should return 404 Not Found for non-existent payment order")
            void shouldReturnNotFoundForNonExistentOrder() {
                // Given
                String nonExistentId = "PO-999999";

                // When/Then
                webTestClient.get()
                        .uri("/payment-initiation/payment-orders/{id}", nonExistentId)
                        .exchange()
                        .expectStatus().isNotFound()
                        .expectHeader().contentType("application/problem+json")
                        .expectBody(ProblemDetail.class)
                        .value(problem -> {
                            assertThat(problem.getTitle()).isEqualTo("Payment Order Not Found");
                            assertThat(problem.getStatus()).isEqualTo(404);
                        });
            }
        }

        @Nested
        @DisplayName("GET /payment-initiation/payment-orders/{id}/status")
        class RetrievePaymentOrderStatusTests {

            @Test
            @Order(4)
            @DisplayName("Should successfully retrieve payment order status")
            void shouldRetrievePaymentOrderStatus() {
                // Given
                assertThat(createdPaymentOrderId).isNotNull();

                // When/Then
                webTestClient.get()
                        .uri("/payment-initiation/payment-orders/{id}/status", createdPaymentOrderId)
                        .exchange()
                        .expectStatus().isOk()
                        .expectHeader().contentType(MediaType.APPLICATION_JSON)
                        .expectBody(PaymentOrderStatusDto.class)
                        .value(status -> {
                            assertThat(status).isNotNull();
                            assertThat(status.getPaymentOrderId()).isEqualTo(createdPaymentOrderId);
                            assertThat(status.getPaymentOrderStatus()).isEqualTo(PaymentStatusDto.PENDING);
                            assertThat(status.getLastUpdatedAt()).isNotNull();
                        });
            }

            @Test
            @DisplayName("Should return 404 Not Found for non-existent order status")
            void shouldReturnNotFoundForNonExistentOrderStatus() {
                // Given
                String nonExistentId = "PO-999999";

                // When/Then
                webTestClient.get()
                        .uri("/payment-initiation/payment-orders/{id}/status", nonExistentId)
                        .exchange()
                        .expectStatus().isNotFound()
                        .expectHeader().contentType("application/problem+json");
            }
        }

        // Helper methods
        private InitiatePaymentOrderRequestDto createValidRequest() {
            InitiatePaymentOrderRequestDto request = new InitiatePaymentOrderRequestDto();
            request.setPaymentOrderReference("REF-" + UUID.randomUUID());
            request.setDebtorAccount("ES7921000813610123456789");
            request.setCreditorAccount("ES1420805801101234567891");

            // Use the REST API Amount class (the generated one)
            AmountDto amount = new AmountDto();
            amount.setAmount(1500.00);
            amount.setCurrency("EUR");
            request.setInstructedAmount(amount);

            request.setRemittanceInformation("Test payment - integration test");
            request.setRequestedExecutionDateTime(LocalDate.now().plusDays(1));

            return request;
        }
    }
}
