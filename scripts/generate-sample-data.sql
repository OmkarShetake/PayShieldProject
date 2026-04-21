-- ============================================================
--   PAYSHIELD — SAMPLE DATA GENERATOR
--   Run this AFTER all services have started and created tables
--   via Flyway migrations.
--
--   Run command:
--   docker exec -i payshield-postgres psql -U postgres < scripts/generate-sample-data.sql
-- ============================================================


-- ============================================================
--   AUTH DB — Users (merchants + admins)
-- ============================================================
\connect auth_db

INSERT INTO users (id, email, password, full_name, role, enabled) VALUES
-- Password for all: Admin@123
('11111111-1111-1111-1111-111111111111', 'merchant1@zomato.com',     '$2a$12$LqIBFiUJjS5Hf7vlMEJfOeZmHunFDLQ3q8QwfpHqJPFuOoaOvBpGG', 'Zomato Payments',    'MERCHANT', true),
('22222222-2222-2222-2222-222222222222', 'merchant2@swiggy.com',     '$2a$12$LqIBFiUJjS5Hf7vlMEJfOeZmHunFDLQ3q8QwfpHqJPFuOoaOvBpGG', 'Swiggy India',       'MERCHANT', true),
('33333333-3333-3333-3333-333333333333', 'merchant3@flipkart.com',   '$2a$12$LqIBFiUJjS5Hf7vlMEJfOeZmHunFDLQ3q8QwfpHqJPFuOoaOvBpGG', 'Flipkart Commerce',  'MERCHANT', true),
('44444444-4444-4444-4444-444444444444', 'merchant4@myntra.com',     '$2a$12$LqIBFiUJjS5Hf7vlMEJfOeZmHunFDLQ3q8QwfpHqJPFuOoaOvBpGG', 'Myntra Fashion',     'MERCHANT', true),
('55555555-5555-5555-5555-555555555555', 'manager@payshield.com',    '$2a$12$LqIBFiUJjS5Hf7vlMEJfOeZmHunFDLQ3q8QwfpHqJPFuOoaOvBpGG', 'Risk Manager',       'ADMIN',    true)
ON CONFLICT (email) DO NOTHING;

INSERT INTO audit_logs (user_id, action, resource, ip_address) VALUES
('11111111-1111-1111-1111-111111111111', 'LOGIN',          '/api/auth/login',    '103.21.58.10'),
('22222222-2222-2222-2222-222222222222', 'LOGIN',          '/api/auth/login',    '103.21.58.11'),
('11111111-1111-1111-1111-111111111111', 'VIEW_DASHBOARD', '/api/reports',       '103.21.58.10'),
('33333333-3333-3333-3333-333333333333', 'LOGIN',          '/api/auth/login',    '122.16.10.45'),
('44444444-4444-4444-4444-444444444444', 'UPDATE_PROFILE', '/api/auth/profile',  '182.68.24.90');


-- ============================================================
--   PAYMENT DB — Merchants + Transactions
-- ============================================================
\connect payment_db

-- Extra merchants (2 already seeded by Flyway)
INSERT INTO merchants (id, name, email, api_key, tier, active) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Zomato Payments',   'merchant1@zomato.com',   'mk_live_zomato_xyz789',   'PREMIUM',    true),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Swiggy India',      'merchant2@swiggy.com',   'mk_live_swiggy_abc456',   'PREMIUM',    true),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Flipkart Commerce', 'merchant3@flipkart.com', 'mk_live_flipkart_def123', 'ENTERPRISE', true),
('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Myntra Fashion',    'merchant4@myntra.com',   'mk_live_myntra_ghi321',   'STANDARD',   true)
ON CONFLICT (email) DO NOTHING;

-- Get the first seeded merchant id
DO $$
DECLARE
    mid UUID;
BEGIN
    SELECT id INTO mid FROM merchants ORDER BY created_at LIMIT 1;

    -- 50 sample transactions across last 30 days
    INSERT INTO transactions (merchant_id, external_ref, amount, currency, status, payment_method, customer_email, customer_phone, description, fraud_score, fraud_flagged, gateway_txn_id, initiated_at, completed_at) VALUES

    -- COMPLETED transactions
    (mid, 'EXT-001', 2500.00,  'INR', 'COMPLETED', 'UPI',          'rahul.sharma@gmail.com',    '9876543210', 'Food Order #1001',        12.5, false, 'GW-TXN-001', NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days' + INTERVAL '2 min'),
    (mid, 'EXT-002', 8999.00,  'INR', 'COMPLETED', 'CARD',         'priya.patel@yahoo.com',      '9123456789', 'Electronics Purchase',    8.2,  false, 'GW-TXN-002', NOW() - INTERVAL '29 days', NOW() - INTERVAL '29 days' + INTERVAL '1 min'),
    (mid, 'EXT-003', 450.00,   'INR', 'COMPLETED', 'WALLET',       'amit.kumar@hotmail.com',    '8765432109', 'Grocery Delivery',        5.1,  false, 'GW-TXN-003', NOW() - INTERVAL '29 days', NOW() - INTERVAL '29 days' + INTERVAL '3 min'),
    (mid, 'EXT-004', 15000.00, 'INR', 'COMPLETED', 'NET_BANKING',  'sneha.singh@gmail.com',     '7654321098', 'Fashion Purchase',        22.3, false, 'GW-TXN-004', NOW() - INTERVAL '28 days', NOW() - INTERVAL '28 days' + INTERVAL '4 min'),
    (mid, 'EXT-005', 3200.00,  'INR', 'COMPLETED', 'UPI',          'vikram.mehta@gmail.com',    '9988776655', 'Restaurant Order',        9.8,  false, 'GW-TXN-005', NOW() - INTERVAL '28 days', NOW() - INTERVAL '28 days' + INTERVAL '2 min'),
    (mid, 'EXT-006', 45000.00, 'INR', 'COMPLETED', 'CARD',         'ananya.roy@gmail.com',      '8877665544', 'Laptop Purchase',         35.6, false, 'GW-TXN-006', NOW() - INTERVAL '27 days', NOW() - INTERVAL '27 days' + INTERVAL '5 min'),
    (mid, 'EXT-007', 1200.00,  'INR', 'COMPLETED', 'UPI',          'rajesh.verma@yahoo.com',    '7766554433', 'Mobile Recharge',         4.2,  false, 'GW-TXN-007', NOW() - INTERVAL '27 days', NOW() - INTERVAL '27 days' + INTERVAL '1 min'),
    (mid, 'EXT-008', 6750.00,  'INR', 'COMPLETED', 'WALLET',       'kavya.nair@gmail.com',      '6655443322', 'Hotel Booking',           18.9, false, 'GW-TXN-008', NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days' + INTERVAL '3 min'),
    (mid, 'EXT-009', 99.00,    'INR', 'COMPLETED', 'UPI',          'suresh.babu@gmail.com',     '9900112233', 'OTT Subscription',        2.1,  false, 'GW-TXN-009', NOW() - INTERVAL '26 days', NOW() - INTERVAL '26 days' + INTERVAL '1 min'),
    (mid, 'EXT-010', 22500.00, 'INR', 'COMPLETED', 'BANK_TRANSFER','meera.krishna@gmail.com',   '8800990011', 'Furniture Purchase',      28.4, false, 'GW-TXN-010', NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days' + INTERVAL '10 min'),
    (mid, 'EXT-011', 550.00,   'INR', 'COMPLETED', 'UPI',          'arjun.pillai@gmail.com',    '7700881122', 'Coffee Shop',             3.5,  false, 'GW-TXN-011', NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days' + INTERVAL '1 min'),
    (mid, 'EXT-012', 12000.00, 'INR', 'COMPLETED', 'CARD',         'divya.menon@yahoo.com',     '6600771133', 'Jewellery Purchase',      45.2, false, 'GW-TXN-012', NOW() - INTERVAL '24 days', NOW() - INTERVAL '24 days' + INTERVAL '4 min'),
    (mid, 'EXT-013', 780.00,   'INR', 'COMPLETED', 'WALLET',       'kiran.rao@gmail.com',       '9977331144', 'Pharmacy',                6.8,  false, 'GW-TXN-013', NOW() - INTERVAL '24 days', NOW() - INTERVAL '24 days' + INTERVAL '2 min'),
    (mid, 'EXT-014', 35000.00, 'INR', 'COMPLETED', 'NET_BANKING',  'pooja.sharma@gmail.com',    '8866220055', 'Travel Booking',          31.7, false, 'GW-TXN-014', NOW() - INTERVAL '23 days', NOW() - INTERVAL '23 days' + INTERVAL '6 min'),
    (mid, 'EXT-015', 4500.00,  'INR', 'COMPLETED', 'UPI',          'rohit.joshi@gmail.com',     '7755110066', 'Electronics Accessories', 14.3, false, 'GW-TXN-015', NOW() - INTERVAL '23 days', NOW() - INTERVAL '23 days' + INTERVAL '2 min'),
    (mid, 'EXT-016', 1800.00,  'INR', 'COMPLETED', 'CARD',         'shreya.gupta@hotmail.com',  '6644000077', 'Books & Stationery',      7.9,  false, 'GW-TXN-016', NOW() - INTERVAL '22 days', NOW() - INTERVAL '22 days' + INTERVAL '2 min'),
    (mid, 'EXT-017', 9800.00,  'INR', 'COMPLETED', 'UPI',          'nikhil.desai@gmail.com',    '9911223344', 'Watches',                 19.5, false, 'GW-TXN-017', NOW() - INTERVAL '22 days', NOW() - INTERVAL '22 days' + INTERVAL '3 min'),
    (mid, 'EXT-018', 250.00,   'INR', 'COMPLETED', 'WALLET',       'tanvi.shah@gmail.com',      '8822334455', 'Street Food',             1.8,  false, 'GW-TXN-018', NOW() - INTERVAL '21 days', NOW() - INTERVAL '21 days' + INTERVAL '1 min'),
    (mid, 'EXT-019', 75000.00, 'INR', 'COMPLETED', 'BANK_TRANSFER','sanjay.patil@gmail.com',    '7733445566', 'MacBook Purchase',        62.4, false, 'GW-TXN-019', NOW() - INTERVAL '21 days', NOW() - INTERVAL '21 days' + INTERVAL '15 min'),
    (mid, 'EXT-020', 5500.00,  'INR', 'COMPLETED', 'CARD',         'anjali.iyer@yahoo.com',     '6644556677', 'Salon Services',          11.2, false, 'GW-TXN-020', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days' + INTERVAL '3 min'),

    -- FAILED transactions
    (mid, 'EXT-021', 3000.00,  'INR', 'FAILED', 'CARD',        'random.user1@gmail.com',    '9999888877', 'Online Shopping',         55.3, false, NULL, NOW() - INTERVAL '20 days', NULL),
    (mid, 'EXT-022', 18000.00, 'INR', 'FAILED', 'NET_BANKING', 'random.user2@yahoo.com',    '8888777766', 'Furniture',               41.8, false, NULL, NOW() - INTERVAL '19 days', NULL),
    (mid, 'EXT-023', 700.00,   'INR', 'FAILED', 'UPI',         'random.user3@gmail.com',    '7777666655', 'Food Delivery',           23.1, false, NULL, NOW() - INTERVAL '19 days', NULL),
    (mid, 'EXT-024', 42000.00, 'INR', 'FAILED', 'CARD',        'random.user4@hotmail.com',  '6666555544', 'Laptop',                  67.9, false, NULL, NOW() - INTERVAL '18 days', NULL),
    (mid, 'EXT-025', 1500.00,  'INR', 'FAILED', 'WALLET',      'random.user5@gmail.com',    '5555444433', 'Mobile Game',             31.5, false, NULL, NOW() - INTERVAL '18 days', NULL),

    -- FRAUD FLAGGED transactions
    (mid, 'EXT-026', 150000.00,'INR', 'FAILED', 'CARD',        'fraud.attempt1@temp.com',   '1111222233', 'Bulk Electronics',        94.7, true,  NULL, NOW() - INTERVAL '17 days', NULL),
    (mid, 'EXT-027', 95000.00, 'INR', 'FAILED', 'CARD',        'fraud.attempt2@temp.com',   '1111222244', 'Gift Cards',              91.2, true,  NULL, NOW() - INTERVAL '16 days', NULL),
    (mid, 'EXT-028', 200000.00,'INR', 'FAILED', 'NET_BANKING', 'fraud.attempt3@temp.com',   '1111222255', 'Wire Transfer',           96.8, true,  NULL, NOW() - INTERVAL '15 days', NULL),
    (mid, 'EXT-029', 78000.00, 'INR', 'FAILED', 'CARD',        'fraud.attempt4@temp.com',   '1111222266', 'Crypto Purchase',         88.3, true,  NULL, NOW() - INTERVAL '14 days', NULL),
    (mid, 'EXT-030', 125000.00,'INR', 'FAILED', 'CARD',        'fraud.attempt5@temp.com',   '1111222277', 'Unknown Merchant',        93.1, true,  NULL, NOW() - INTERVAL '13 days', NULL),

    -- Recent transactions (last 7 days)
    (mid, 'EXT-031', 3500.00,  'INR', 'COMPLETED', 'UPI',      'user31@gmail.com',          '9000100031', 'Food Delivery',           8.4,  false, 'GW-TXN-031', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days' + INTERVAL '2 min'),
    (mid, 'EXT-032', 6200.00,  'INR', 'COMPLETED', 'CARD',     'user32@gmail.com',          '9000100032', 'Clothing',                15.7, false, 'GW-TXN-032', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days' + INTERVAL '3 min'),
    (mid, 'EXT-033', 14500.00, 'INR', 'COMPLETED', 'UPI',      'user33@gmail.com',          '9000100033', 'Appliances',              29.3, false, 'GW-TXN-033', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days' + INTERVAL '4 min'),
    (mid, 'EXT-034', 850.00,   'INR', 'COMPLETED', 'WALLET',   'user34@gmail.com',          '9000100034', 'Coffee',                  3.2,  false, 'GW-TXN-034', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days' + INTERVAL '1 min'),
    (mid, 'EXT-035', 28000.00, 'INR', 'COMPLETED', 'NET_BANKING','user35@gmail.com',        '9000100035', 'Flight Ticket',           33.8, false, 'GW-TXN-035', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days' + INTERVAL '5 min'),
    (mid, 'EXT-036', 2100.00,  'INR', 'COMPLETED', 'UPI',      'user36@gmail.com',          '9000100036', 'Medicine',                7.1,  false, 'GW-TXN-036', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days' + INTERVAL '2 min'),
    (mid, 'EXT-037', 55000.00, 'INR', 'COMPLETED', 'CARD',     'user37@gmail.com',          '9000100037', 'TV Purchase',             48.6, false, 'GW-TXN-037', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days' + INTERVAL '6 min'),
    (mid, 'EXT-038', 4800.00,  'INR', 'COMPLETED', 'UPI',      'user38@gmail.com',          '9000100038', 'Sports Equipment',        11.4, false, 'GW-TXN-038', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days' + INTERVAL '2 min'),
    (mid, 'EXT-039', 1100.00,  'INR', 'FAILED',    'CARD',     'user39@gmail.com',          '9000100039', 'Game Credits',            42.7, false, NULL,          NOW() - INTERVAL '2 days', NULL),
    (mid, 'EXT-040', 9200.00,  'INR', 'COMPLETED', 'WALLET',   'user40@gmail.com',          '9000100040', 'Gadgets',                 16.9, false, 'GW-TXN-040', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '3 min'),
    (mid, 'EXT-041', 3750.00,  'INR', 'COMPLETED', 'UPI',      'user41@gmail.com',          '9000100041', 'Grocery',                 6.3,  false, 'GW-TXN-041', NOW() - INTERVAL '1 day',  NOW() - INTERVAL '1 day'  + INTERVAL '1 min'),
    (mid, 'EXT-042', 18900.00, 'INR', 'COMPLETED', 'CARD',     'user42@gmail.com',          '9000100042', 'Camera',                  24.5, false, 'GW-TXN-042', NOW() - INTERVAL '1 day',  NOW() - INTERVAL '1 day'  + INTERVAL '4 min'),
    (mid, 'EXT-043', 600.00,   'INR', 'COMPLETED', 'UPI',      'user43@gmail.com',          '9000100043', 'Snacks',                  2.8,  false, 'GW-TXN-043', NOW() - INTERVAL '12 hours',NOW() - INTERVAL '12 hours' + INTERVAL '1 min'),
    (mid, 'EXT-044', 42000.00, 'INR', 'COMPLETED', 'NET_BANKING','user44@gmail.com',        '9000100044', 'iPad Purchase',           39.7, false, 'GW-TXN-044', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours'  + INTERVAL '8 min'),
    (mid, 'EXT-045', 1650.00,  'INR', 'COMPLETED', 'WALLET',   'user45@gmail.com',          '9000100045', 'Books',                   5.5,  false, 'GW-TXN-045', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours'  + INTERVAL '2 min'),
    (mid, 'EXT-046', 7300.00,  'INR', 'COMPLETED', 'CARD',     'user46@gmail.com',          '9000100046', 'Shoes',                   13.8, false, 'GW-TXN-046', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'  + INTERVAL '3 min'),
    (mid, 'EXT-047', 25000.00, 'INR', 'COMPLETED', 'UPI',      'user47@gmail.com',          '9000100047', 'Smart Watch',             27.2, false, 'GW-TXN-047', NOW() - INTERVAL '1 hour',  NOW() - INTERVAL '1 hour'   + INTERVAL '3 min'),
    (mid, 'EXT-048', 88000.00, 'INR', 'FAILED',    'CARD',     'fraud.new1@temp.com',       '1111999901', 'Bulk Order',              89.4, true,  NULL,          NOW() - INTERVAL '45 min', NULL),
    (mid, 'EXT-049', 3300.00,  'INR', 'COMPLETED', 'UPI',      'user49@gmail.com',          '9000100049', 'Dinner',                  4.6,  false, 'GW-TXN-049', NOW() - INTERVAL '20 min', NOW() - INTERVAL '20 min'   + INTERVAL '1 min'),
    (mid, 'EXT-050', 11500.00, 'INR', 'COMPLETED', 'CARD',     'user50@gmail.com',          '9000100050', 'Perfume',                 18.3, false, 'GW-TXN-050', NOW() - INTERVAL '5 min',  NOW() - INTERVAL '5 min'    + INTERVAL '2 min');

END $$;


-- ============================================================
--   FRAUD DB — Scores + Alerts
-- ============================================================
\connect fraud_db

-- Get merchant id from a placeholder (fraud DB doesn't have merchants table,
-- we store merchant_id from payment events)
INSERT INTO fraud_scores (transaction_id, merchant_id, score, flagged, model_version, rule_triggers, decision, features) VALUES
('00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 94.7, true,  'v2.1', '["HIGH_AMOUNT","VELOCITY_COUNT"]',   'REJECT', '{"amount":150000,"hour":2,"device":"new"}'),
('00000001-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 91.2, true,  'v2.1', '["HIGH_AMOUNT","NEW_DEVICE"]',        'REJECT', '{"amount":95000,"hour":23,"device":"new"}'),
('00000001-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 96.8, true,  'v2.1', '["HIGH_AMOUNT","GEO_MISMATCH"]',      'REJECT', '{"amount":200000,"country":"NG","device":"known"}'),
('00000001-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 88.3, true,  'v2.1', '["HIGH_AMOUNT","CARD_TESTING"]',      'REJECT', '{"amount":78000,"attempts":5,"device":"new"}'),
('00000001-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', 93.1, true,  'v2.1', '["HIGH_AMOUNT","VELOCITY_COUNT"]',    'REJECT', '{"amount":125000,"hour":3,"txns_1hr":8}'),
('00000001-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000001', 72.4, false, 'v2.1', '["VELOCITY_COUNT"]',                  'FLAG',   '{"amount":18000,"txns_1hr":7,"device":"known"}'),
('00000001-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000001', 68.9, false, 'v2.1', '["HIGH_AMOUNT","NIGHT_TRANSACTION"]', 'FLAG',   '{"amount":45000,"hour":3,"device":"known"}'),
('00000001-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000001', 55.3, false, 'v2.1', '["NIGHT_TRANSACTION","ROUND_AMOUNT"]', 'REVIEW', '{"amount":3000,"hour":4,"device":"known"}'),
('00000001-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000001', 12.5, false, 'v2.1', '[]',                                  'APPROVE','{"amount":2500,"hour":14,"device":"known"}'),
('00000001-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000001', 8.2,  false, 'v2.1', '[]',                                  'APPROVE','{"amount":8999,"hour":11,"device":"known"}');

INSERT INTO fraud_alerts (transaction_id, merchant_id, alert_type, severity, description, resolved) VALUES
('00000001-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'HIGH_AMOUNT_NIGHT',  'CRITICAL', 'Transaction of ₹1,50,000 at 2 AM from new device. Score: 94.7',  false),
('00000001-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'NEW_DEVICE_LARGE',   'HIGH',     'Large transaction ₹95,000 from unrecognized device. Score: 91.2', false),
('00000001-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'GEO_MISMATCH',       'CRITICAL', 'Transaction originated from Nigeria, customer is India-based.',   false),
('00000001-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'CARD_TESTING',       'HIGH',     '5 failed attempts before this ₹78,000 transaction. Score: 88.3', true),
('00000001-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', 'VELOCITY_BREACH',    'CRITICAL', '8 transactions in past hour, total ₹1,25,000. Score: 93.1',       false),
('00000001-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000001', 'VELOCITY_WARNING',   'MEDIUM',   '7 transactions in 1 hour from same customer. Score: 72.4',        false),
('00000001-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000001', 'LARGE_NIGHT_TXN',    'MEDIUM',   '₹45,000 transaction at 3 AM. Score: 68.9',                        true),
('00000001-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000001', 'SUSPICIOUS_PATTERN', 'LOW',      'Round amount at unusual hour. Score: 55.3',                        true);


-- ============================================================
--   RECON DB — Settlements + Recon Records
-- ============================================================
\connect recon_db

INSERT INTO settlements (id, bank_ref, merchant_id, amount, currency, settled_at, bank_name) VALUES
('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', 'HDFC-SETTLE-001', '00000000-0000-0000-0000-000000000001', 285000.00, 'INR', NOW() - INTERVAL '25 days', 'HDFC Bank'),
('b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', 'HDFC-SETTLE-002', '00000000-0000-0000-0000-000000000001', 432500.00, 'INR', NOW() - INTERVAL '18 days', 'HDFC Bank'),
('c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 'ICICI-SETTLE-001','00000000-0000-0000-0000-000000000001', 178000.00, 'INR', NOW() - INTERVAL '11 days', 'ICICI Bank'),
('d4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4', 'SBI-SETTLE-001',  '00000000-0000-0000-0000-000000000001', 95000.00,  'INR', NOW() - INTERVAL '4 days',  'State Bank of India'),
('e5e5e5e5-e5e5-e5e5-e5e5-e5e5e5e5e5e5', 'AXIS-SETTLE-001', '00000000-0000-0000-0000-000000000001', 312000.00, 'INR', NOW() - INTERVAL '1 day',   'Axis Bank');

-- Recon run 1 (completed, high match rate)
INSERT INTO recon_runs (id, merchant_id, status, from_date, to_date, total_txns, matched, mismatched, missing, started_at, completed_at, triggered_by) VALUES
('run00001-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'COMPLETED', NOW()-INTERVAL '30 days', NOW()-INTERVAL '25 days', 120, 117, 2, 1, NOW()-INTERVAL '24 days', NOW()-INTERVAL '24 days' + INTERVAL '5 min', 'admin@payshield.com'),
('run00002-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'COMPLETED', NOW()-INTERVAL '25 days', NOW()-INTERVAL '18 days', 98,  95,  2, 1, NOW()-INTERVAL '17 days', NOW()-INTERVAL '17 days' + INTERVAL '4 min', 'admin@payshield.com'),
('run00003-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'COMPLETED', NOW()-INTERVAL '18 days', NOW()-INTERVAL '11 days', 145, 143, 1, 1, NOW()-INTERVAL '10 days', NOW()-INTERVAL '10 days' + INTERVAL '6 min', 'admin@payshield.com'),
('run00004-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'COMPLETED', NOW()-INTERVAL '7 days',  NOW()-INTERVAL '1 day',   87,  85,  1, 1, NOW()-INTERVAL '1 day',  NOW()-INTERVAL '1 day'   + INTERVAL '3 min', 'admin@payshield.com');

-- Recon records (mix of MATCHED and MISMATCH)
INSERT INTO recon_records (transaction_id, settlement_id, merchant_id, txn_amount, settlement_amount, delta, status, mismatch_reason, run_id) VALUES
('00000001-0000-0000-0000-000000000001', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', '00000000-0000-0000-0000-000000000001', 2500.00,  2500.00,  0.00,  'MATCHED',  NULL, 'run00001-0000-0000-0000-000000000001'),
('00000001-0000-0000-0000-000000000002', 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', '00000000-0000-0000-0000-000000000001', 8999.00,  8999.00,  0.00,  'MATCHED',  NULL, 'run00001-0000-0000-0000-000000000001'),
('00000001-0000-0000-0000-000000000003', 'b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', '00000000-0000-0000-0000-000000000001', 15000.00, 14980.00, 20.00, 'MISMATCH', 'Bank deducted processing fee', 'run00002-0000-0000-0000-000000000002'),
('00000001-0000-0000-0000-000000000004', 'b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', '00000000-0000-0000-0000-000000000001', 45000.00, 45000.00, 0.00,  'MATCHED',  NULL, 'run00002-0000-0000-0000-000000000002'),
('00000001-0000-0000-0000-000000000005', 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', '00000000-0000-0000-0000-000000000001', 75000.00, 74950.00, 50.00, 'MISMATCH', 'Settlement amount differs by ₹50', 'run00003-0000-0000-0000-000000000003'),
('00000001-0000-0000-0000-000000000006', 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', '00000000-0000-0000-0000-000000000001', 35000.00, 35000.00, 0.00,  'MATCHED',  NULL, 'run00003-0000-0000-0000-000000000003'),
('00000001-0000-0000-0000-000000000007', 'd4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4', '00000000-0000-0000-0000-000000000001', 3500.00,  3500.00,  0.00,  'MATCHED',  NULL, 'run00004-0000-0000-0000-000000000004'),
('00000001-0000-0000-0000-000000000008', 'd4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4', '00000000-0000-0000-0000-000000000001', 9200.00,  9200.00,  0.00,  'MATCHED',  NULL, 'run00004-0000-0000-0000-000000000004');


-- ============================================================
--   NOTIFICATION DB — Notification logs
-- ============================================================
\connect notification_db

INSERT INTO notifications (type, recipient, subject, body, template_name, status, attempts, reference_id, reference_type, sent_at) VALUES
('EMAIL', 'rahul.sharma@gmail.com',   'Payment Successful - ₹2,500',    'Your payment of ₹2,500 was successful. Txn ID: EXT-001',           'payment_success', 'SENT',   1, 'EXT-001', 'TRANSACTION', NOW()-INTERVAL '30 days'),
('EMAIL', 'priya.patel@yahoo.com',    'Payment Successful - ₹8,999',    'Your payment of ₹8,999 was successful. Txn ID: EXT-002',           'payment_success', 'SENT',   1, 'EXT-002', 'TRANSACTION', NOW()-INTERVAL '29 days'),
('EMAIL', 'random.user1@gmail.com',   'Payment Failed - ₹3,000',        'Your payment of ₹3,000 failed. Please retry. Txn ID: EXT-021',    'payment_failed',  'SENT',   1, 'EXT-021', 'TRANSACTION', NOW()-INTERVAL '20 days'),
('EMAIL', 'fraud.attempt1@temp.com',  'Transaction Blocked',             'Your transaction has been blocked due to suspicious activity.',    'fraud_blocked',   'SENT',   1, 'EXT-026', 'TRANSACTION', NOW()-INTERVAL '17 days'),
('EMAIL', 'fraud.attempt2@temp.com',  'Transaction Blocked',             'Your transaction has been blocked for security reasons.',          'fraud_blocked',   'SENT',   1, 'EXT-027', 'TRANSACTION', NOW()-INTERVAL '16 days'),
('EMAIL', 'sanjay.patil@gmail.com',   'Payment Successful - ₹75,000',   'Your payment of ₹75,000 was successful. Txn ID: EXT-019',         'payment_success', 'SENT',   1, 'EXT-019', 'TRANSACTION', NOW()-INTERVAL '21 days'),
('EMAIL', 'user37@gmail.com',         'Payment Successful - ₹55,000',   'Your payment of ₹55,000 was successful. Txn ID: EXT-037',         'payment_success', 'SENT',   1, 'EXT-037', 'TRANSACTION', NOW()-INTERVAL '3 days'),
('EMAIL', 'user48@gmail.com',         'Transaction Blocked',             'Your transaction of ₹88,000 has been blocked due to high risk.',  'fraud_blocked',   'SENT',   1, 'EXT-048', 'TRANSACTION', NOW()-INTERVAL '45 min'),
('EMAIL', 'user50@gmail.com',         'Payment Successful - ₹11,500',   'Your payment of ₹11,500 was successful. Txn ID: EXT-050',         'payment_success', 'SENT',   1, 'EXT-050', 'TRANSACTION', NOW()-INTERVAL '5 min'),
('EMAIL', 'admin@payshield.com',      'Daily Reconciliation Complete',   'Reconciliation run completed. 85/87 matched (97.7% match rate)',  'recon_complete',  'SENT',   1, 'run00004-0000-0000-0000-000000000004', 'RECON_RUN', NOW()-INTERVAL '1 day'),
('EMAIL', 'random.user3@gmail.com',   'Payment Failed - ₹700',          'Your payment of ₹700 failed. Please retry. Txn ID: EXT-023',     'payment_failed',  'FAILED', 3, 'EXT-023', 'TRANSACTION', NULL),
('SMS',   '9876543210',               NULL,                               'PayShield: ₹2,500 paid successfully. Ref: EXT-001',              'sms_success',     'SENT',   1, 'EXT-001', 'TRANSACTION', NOW()-INTERVAL '30 days'),
('SMS',   '8765432109',               NULL,                               'PayShield: ₹450 paid successfully. Ref: EXT-003',               'sms_success',     'SENT',   1, 'EXT-003', 'TRANSACTION', NOW()-INTERVAL '29 days'),
('WEBHOOK','https://hooks.zomato.com/payshield', 'payment.completed',    '{"txn":"EXT-001","amount":2500,"status":"COMPLETED"}',           'webhook_event',   'SENT',   1, 'EXT-001', 'TRANSACTION', NOW()-INTERVAL '30 days'),
('WEBHOOK','https://hooks.zomato.com/payshield', 'payment.failed',       '{"txn":"EXT-021","amount":3000,"status":"FAILED"}',             'webhook_event',   'SENT',   1, 'EXT-021', 'TRANSACTION', NOW()-INTERVAL '20 days');


-- ============================================================
--   REPORTING DB — Daily summaries + Payment method stats
-- ============================================================
\connect reporting_db

INSERT INTO transaction_summary (merchant_id, summary_date, total_transactions, successful, failed, flagged_fraud, total_volume, avg_transaction, success_rate) VALUES
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 29, 45, 42, 3, 0, 185000.00, 4111.11, 93.33),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 28, 52, 49, 3, 1, 242000.00, 4653.84, 94.23),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 27, 38, 35, 3, 0, 156000.00, 4105.26, 92.10),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 26, 61, 58, 3, 2, 298000.00, 4885.24, 95.08),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 25, 44, 41, 3, 1, 198000.00, 4500.00, 93.18),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 24, 55, 52, 3, 0, 267000.00, 4854.54, 94.54),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 23, 49, 46, 3, 1, 235000.00, 4795.91, 93.87),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 22, 67, 63, 4, 2, 315000.00, 4701.49, 94.02),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 21, 41, 38, 3, 0, 189000.00, 4609.75, 92.68),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 20, 58, 54, 4, 2, 278000.00, 4793.10, 93.10),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 19, 53, 50, 3, 1, 254000.00, 4792.45, 94.33),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 18, 72, 68, 4, 3, 358000.00, 4972.22, 94.44),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 17, 46, 43, 3, 1, 218000.00, 4739.13, 93.47),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 16, 63, 60, 3, 1, 312000.00, 4952.38, 95.23),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 15, 57, 53, 4, 2, 275000.00, 4824.56, 92.98),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 14, 48, 45, 3, 0, 231000.00, 4812.50, 93.75),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 13, 69, 65, 4, 2, 342000.00, 4956.52, 94.20),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 12, 54, 51, 3, 1, 261000.00, 4833.33, 94.44),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 11, 76, 72, 4, 2, 389000.00, 5118.42, 94.73),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 10, 51, 48, 3, 1, 246000.00, 4823.52, 94.11),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 9,  65, 62, 3, 2, 321000.00, 4938.46, 95.38),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 8,  59, 56, 3, 0, 287000.00, 4864.40, 94.91),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 7,  83, 79, 4, 3, 412000.00, 4963.85, 95.18),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 6,  71, 67, 4, 2, 356000.00, 5014.08, 94.36),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 5,  88, 84, 4, 2, 445000.00, 5056.81, 95.45),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 4,  74, 70, 4, 3, 371000.00, 5013.51, 94.59),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 3,  92, 88, 4, 2, 468000.00, 5086.95, 95.65),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 2,  68, 65, 3, 1, 342000.00, 5029.41, 95.58),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE - 1,  95, 91, 4, 3, 487000.00, 5126.31, 95.78),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE,      42, 40, 2, 1, 215000.00, 5119.04, 95.23)
ON CONFLICT (merchant_id, summary_date) DO NOTHING;

INSERT INTO payment_method_stats (merchant_id, summary_date, payment_method, count, volume) VALUES
-- Today
('00000000-0000-0000-0000-000000000001', CURRENT_DATE, 'UPI',          18, 82000.00),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE, 'CARD',         13, 95000.00),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE, 'NET_BANKING',  5,  28000.00),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE, 'WALLET',       4,  7200.00),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE, 'BANK_TRANSFER',2,  2800.00),
-- Yesterday
('00000000-0000-0000-0000-000000000001', CURRENT_DATE-1, 'UPI',          40, 152000.00),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE-1, 'CARD',         29, 142000.00),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE-1, 'NET_BANKING',  14, 82000.00),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE-1, 'WALLET',       8,  11000.00),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE-1, 'BANK_TRANSFER',4,  0.00),
-- 7 days ago
('00000000-0000-0000-0000-000000000001', CURRENT_DATE-7, 'UPI',          35, 142000.00),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE-7, 'CARD',         26, 158000.00),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE-7, 'NET_BANKING',  12, 72000.00),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE-7, 'WALLET',       7,  22000.00),
('00000000-0000-0000-0000-000000000001', CURRENT_DATE-7, 'BANK_TRANSFER',3,  18000.00)
ON CONFLICT (merchant_id, summary_date, payment_method) DO NOTHING;


-- ============================================================
--   DONE
-- ============================================================
\echo ''
\echo '✔ PayShield sample data loaded successfully!'
\echo ''
\echo 'Summary:'
\echo '  auth_db        → 5 users + 5 audit logs'
\echo '  payment_db     → 4 merchants + 50 transactions'
\echo '  fraud_db       → 10 fraud scores + 8 fraud alerts'
\echo '  recon_db       → 5 settlements + 4 recon runs + 8 records'
\echo '  notification_db → 15 notifications (email + sms + webhook)'
\echo '  reporting_db   → 30 daily summaries + 15 payment method stats'
\echo ''
