-- Payment Service Database Schema

CREATE TABLE merchants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    api_key         VARCHAR(255) NOT NULL UNIQUE,
    webhook_url     VARCHAR(500),
    tier            VARCHAR(50)  NOT NULL DEFAULT 'STANDARD',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id         UUID NOT NULL REFERENCES merchants(id),
    external_ref        VARCHAR(255) UNIQUE,
    amount              NUMERIC(18,2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    status              VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    payment_method      VARCHAR(20) NOT NULL,
    customer_email      VARCHAR(255),
    customer_phone      VARCHAR(20),
    description         VARCHAR(500),
    fraud_score         NUMERIC(5,2),
    fraud_flagged       BOOLEAN NOT NULL DEFAULT FALSE,
    gateway_txn_id      VARCHAR(255),
    gateway_response    JSONB,
    metadata            JSONB,
    initiated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE transaction_events (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    event_type      VARCHAR(100) NOT NULL,
    old_status      VARCHAR(50),
    new_status      VARCHAR(50),
    details         JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE refunds (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    amount          NUMERIC(18,2) NOT NULL,
    reason          VARCHAR(500),
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed merchants
INSERT INTO merchants (name, email, api_key, tier) VALUES
('Demo Merchant',  'demo@merchant.com',  'mk_test_demo123456789',  'PREMIUM'),
('Test Store',     'test@store.com',     'mk_test_store987654321', 'STANDARD');

CREATE INDEX idx_transactions_merchant_id   ON transactions(merchant_id);
CREATE INDEX idx_transactions_status        ON transactions(status);
CREATE INDEX idx_transactions_initiated_at  ON transactions(initiated_at DESC);
CREATE INDEX idx_transactions_fraud_flagged ON transactions(fraud_flagged) WHERE fraud_flagged = TRUE;
CREATE INDEX idx_transaction_events_txn_id  ON transaction_events(transaction_id);
