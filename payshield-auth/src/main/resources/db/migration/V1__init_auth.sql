CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL DEFAULT 'MERCHANT',
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID REFERENCES users(id),
    action      VARCHAR(100) NOT NULL,
    resource    VARCHAR(255),
    ip_address  VARCHAR(50),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed admin user (password: Admin@123)
INSERT INTO users (email, password, full_name, role) VALUES
('admin@payshield.com',
 '$2a$12$LqIBFiUJjS5Hf7vlMEJfOeZmHunFDLQ3q8QwfpHqJPFuOoaOvBpGG',
 'Platform Admin', 'ADMIN');

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
