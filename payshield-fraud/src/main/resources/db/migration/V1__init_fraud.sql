CREATE TABLE fraud_scores (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL UNIQUE,
    merchant_id     UUID NOT NULL,
    score           NUMERIC(5,2) NOT NULL,
    flagged         BOOLEAN NOT NULL DEFAULT FALSE,
    model_version   VARCHAR(50) NOT NULL DEFAULT 'v1.0',
    features        JSONB,
    rule_triggers   JSONB,
    decision        VARCHAR(50) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE fraud_rules (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    rule_type   VARCHAR(50) NOT NULL,
    condition   JSONB NOT NULL,
    score_weight NUMERIC(5,2) NOT NULL DEFAULT 10.0,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE fraud_alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL,
    merchant_id     UUID NOT NULL,
    alert_type      VARCHAR(100) NOT NULL,
    severity        VARCHAR(20) NOT NULL DEFAULT 'HIGH',
    description     TEXT,
    resolved        BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at     TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE velocity_tracking (
    id              BIGSERIAL PRIMARY KEY,
    merchant_id     UUID NOT NULL,
    customer_email  VARCHAR(255),
    customer_phone  VARCHAR(20),
    transaction_count INT NOT NULL DEFAULT 0,
    total_amount    NUMERIC(18,2) NOT NULL DEFAULT 0,
    window_start    TIMESTAMP NOT NULL,
    window_end      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed default rules
INSERT INTO fraud_rules (name, description, rule_type, condition, score_weight) VALUES
('HIGH_AMOUNT', 'Transaction amount exceeds threshold', 'AMOUNT', '{"threshold": 100000}', 30.0),
('VELOCITY_COUNT', 'Too many transactions in short period', 'VELOCITY', '{"max_per_hour": 10}', 40.0),
('VELOCITY_AMOUNT', 'Too much total amount in short period', 'VELOCITY', '{"max_amount_per_hour": 500000}', 35.0),
('NIGHT_TRANSACTION', 'Transaction at unusual hours (2am-5am)', 'TIME', '{"hours": [2,3,4,5]}', 15.0),
('ROUND_AMOUNT', 'Suspiciously round amount', 'PATTERN', '{"pattern": "round_number"}', 10.0);

CREATE INDEX idx_fraud_scores_transaction_id ON fraud_scores(transaction_id);
CREATE INDEX idx_fraud_scores_merchant_id ON fraud_scores(merchant_id);
CREATE INDEX idx_fraud_scores_flagged ON fraud_scores(flagged) WHERE flagged = TRUE;
CREATE INDEX idx_fraud_alerts_merchant_id ON fraud_alerts(merchant_id);
CREATE INDEX idx_velocity_merchant_window ON velocity_tracking(merchant_id, window_start, window_end);
