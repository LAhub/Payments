-- src/main/resources/schema.sql

-- Payment Orders Table
CREATE TABLE IF NOT EXISTS payment_orders (
    id BIGSERIAL PRIMARY KEY,
    payment_order_id VARCHAR(100) UNIQUE NOT NULL,
    payment_order_reference VARCHAR(100) NOT NULL,
    debtor_account VARCHAR(34) NOT NULL,
    creditor_account VARCHAR(34) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    remittance_information VARCHAR(500),
    requested_execution_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_updated_at TIMESTAMP NOT NULL,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_status_valid CHECK (status IN ('PENDING', 'PROCESSING', 'SETTLED', 'REJECTED', 'CANCELLED'))
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_payment_order_id ON payment_orders(payment_order_id);
CREATE INDEX IF NOT EXISTS idx_payment_order_reference ON payment_orders(payment_order_reference);
CREATE INDEX IF NOT EXISTS idx_payment_order_status ON payment_orders(status);
CREATE INDEX IF NOT EXISTS idx_payment_order_created_at ON payment_orders(created_at);

-- Idempotency Keys Table
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    payment_order_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_order FOREIGN KEY (payment_order_id) 
        REFERENCES payment_orders(payment_order_id) ON DELETE CASCADE
);

-- Indexes for idempotency
CREATE INDEX IF NOT EXISTS idx_idempotency_key ON idempotency_keys(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_idempotency_expires ON idempotency_keys(expires_at);

-- Optional: Create function to clean expired idempotency keys
CREATE OR REPLACE FUNCTION clean_expired_idempotency_keys()
RETURNS void AS $$
BEGIN
    DELETE FROM idempotency_keys WHERE expires_at < NOW();
END;
$$ LANGUAGE plpgsql;
