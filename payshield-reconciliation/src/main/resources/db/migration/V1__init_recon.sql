-- recon_status stored as VARCHAR(20)

CREATE TABLE settlements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_ref        VARCHAR(255) NOT NULL UNIQUE,
    merchant_id     UUID NOT NULL,
    amount          NUMERIC(18,2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'INR',
    settled_at      TIMESTAMP NOT NULL,
    bank_name       VARCHAR(100),
    raw_data        JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE recon_records (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id      UUID,
    settlement_id       UUID REFERENCES settlements(id),
    merchant_id         UUID NOT NULL,
    txn_amount          NUMERIC(18,2),
    settlement_amount   NUMERIC(18,2),
    delta               NUMERIC(18,2),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    mismatch_reason     VARCHAR(500),
    resolved            BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at         TIMESTAMP,
    resolved_by         VARCHAR(255),
    run_id              UUID NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE recon_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id     UUID NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
    from_date       TIMESTAMP NOT NULL,
    to_date         TIMESTAMP NOT NULL,
    total_txns      INT DEFAULT 0,
    matched         INT DEFAULT 0,
    mismatched      INT DEFAULT 0,
    missing         INT DEFAULT 0,
    started_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP,
    triggered_by    VARCHAR(255)
);

CREATE INDEX idx_settlements_merchant_id ON settlements(merchant_id);
CREATE INDEX idx_settlements_settled_at ON settlements(settled_at DESC);
CREATE INDEX idx_recon_records_run_id ON recon_records(run_id);
CREATE INDEX idx_recon_records_merchant_id ON recon_records(merchant_id);
CREATE INDEX idx_recon_records_status ON recon_records(status);
CREATE INDEX idx_recon_runs_merchant_id ON recon_runs(merchant_id);
