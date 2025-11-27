package com.ec.bank.payments.infrastructure.adapter.output.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * R2DBC entity for payment orders.
 * Maps to payment_orders table in PostgreSQL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payment_orders")
public class PaymentOrderEntity {
    
    @Id
    @Column("id")
    private Long id;
    
    @Column("payment_order_id")
    private String paymentOrderId;
    
    @Column("payment_order_reference")
    private String paymentOrderReference;
    
    @Column("debtor_account")
    private String debtorAccount;
    
    @Column("creditor_account")
    private String creditorAccount;
    
    @Column("amount")
    private BigDecimal amount;
    
    @Column("currency")
    private String currency;
    
    @Column("remittance_information")
    private String remittanceInformation;
    
    @Column("requested_execution_date")
    private LocalDate requestedExecutionDate;
    
    @Column("status")
    private String status;
    
    @Column("created_at")
    private Instant createdAt;
    
    @Column("last_updated_at")
    private Instant lastUpdatedAt;
    
    @Version
    @Column("version")
    private Long version;
}
