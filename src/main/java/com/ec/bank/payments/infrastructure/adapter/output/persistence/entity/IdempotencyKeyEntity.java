package com.ec.bank.payments.infrastructure.adapter.output.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * R2DBC entity for idempotency keys.
 * Maps to idempotency_keys table in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("idempotency_keys")
public class IdempotencyKeyEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("idempotency_key")
    private String idempotencyKey;

    @Column("payment_order_id")
    private String paymentOrderId;

    @Column("created_at")
    private Instant createdAt;

    @Column("expires_at")
    private Instant expiresAt;
}
