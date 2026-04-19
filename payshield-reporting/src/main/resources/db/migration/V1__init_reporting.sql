CREATE TABLE transaction_summary (
                                     id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     merchant_id         UUID NOT NULL,
                                     summary_date        DATE NOT NULL,
                                     total_transactions  BIGINT NOT NULL DEFAULT 0,
                                     successful          BIGINT NOT NULL DEFAULT 0,
                                     failed              BIGINT NOT NULL DEFAULT 0,
                                     flagged_fraud       BIGINT NOT NULL DEFAULT 0,
                                     total_volume        NUMERIC(18,2) NOT NULL DEFAULT 0,
                                     avg_transaction     NUMERIC(18,2) NOT NULL DEFAULT 0,
                                     success_rate        NUMERIC(5,2) NOT NULL DEFAULT 0,
                                     created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                     updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                                     UNIQUE(merchant_id, summary_date)
);

CREATE TABLE payment_method_stats (
                                      id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      merchant_id     UUID NOT NULL,
                                      summary_date    DATE NOT NULL,
                                      payment_method  VARCHAR(50) NOT NULL,
                                      count           BIGINT NOT NULL DEFAULT 0,
                                      volume          NUMERIC(18,2) NOT NULL DEFAULT 0,
                                      created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                                      UNIQUE(merchant_id, summary_date, payment_method)
);

CREATE INDEX idx_txn_summary_merchant_date ON transaction_summary(merchant_id, summary_date DESC);
CREATE INDEX idx_pm_stats_merchant_date ON payment_method_stats(merchant_id, summary_date DESC);