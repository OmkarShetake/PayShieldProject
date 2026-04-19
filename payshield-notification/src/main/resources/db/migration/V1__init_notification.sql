-- notification_type stored as VARCHAR(20)
-- notification_status stored as VARCHAR(20)

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type            VARCHAR(20) NOT NULL,
    recipient       VARCHAR(500) NOT NULL,
    subject         VARCHAR(500),
    body            TEXT NOT NULL,
    template_name   VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts        INT NOT NULL DEFAULT 0,
    max_attempts    INT NOT NULL DEFAULT 3,
    error_message   TEXT,
    reference_id    VARCHAR(255),
    reference_type  VARCHAR(100),
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE webhook_endpoints (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    url         VARCHAR(500) NOT NULL,
    secret      VARCHAR(255),
    events      TEXT[],
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_reference ON notifications(reference_id, reference_type);
CREATE INDEX idx_webhook_merchant_id ON webhook_endpoints(merchant_id);
