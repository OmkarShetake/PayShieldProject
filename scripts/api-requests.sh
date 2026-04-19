# PayShield API — Sample Requests (HTTPie / curl format)
# Base URL: http://localhost:8080

# ─── 1. Register a new merchant user ────────────────────────────────────────
# POST /api/auth/register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "merchant@example.com",
    "password": "Merchant@123",
    "fullName": "Test Merchant",
    "role": "MERCHANT"
  }'

# ─── 2. Login ────────────────────────────────────────────────────────────────
# POST /api/auth/login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@payshield.com",
    "password": "Admin@123"
  }'
# Response: { "accessToken": "...", "refreshToken": "...", "user": {...} }
# Save the accessToken as TOKEN

TOKEN="<paste_access_token_here>"
MERCHANT_ID="<paste_merchant_uuid_here>"

# ─── 3. Initiate a payment ───────────────────────────────────────────────────
curl -X POST http://localhost:8080/api/payments/initiate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Merchant-Id: $MERCHANT_ID" \
  -d '{
    "amount": 15000.00,
    "currency": "INR",
    "paymentMethod": "UPI",
    "customerEmail": "customer@example.com",
    "customerPhone": "+919876543210",
    "description": "Order #1234",
    "externalRef": "ORD-1234"
  }'

# ─── 4. Process (confirm) a payment ─────────────────────────────────────────
TXN_ID="<paste_transaction_id>"
curl -X POST http://localhost:8080/api/payments/$TXN_ID/process \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Merchant-Id: $MERCHANT_ID"

# ─── 5. Get transaction status ───────────────────────────────────────────────
curl http://localhost:8080/api/payments/$TXN_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Merchant-Id: $MERCHANT_ID"

# ─── 6. List transactions ────────────────────────────────────────────────────
curl "http://localhost:8080/api/payments?page=0&size=20&status=COMPLETED" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Merchant-Id: $MERCHANT_ID"

# ─── 7. Manual fraud score check ────────────────────────────────────────────
curl -X POST http://localhost:8080/api/fraud/score \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "00000000-0000-0000-0000-000000000001",
    "merchantId": "'$MERCHANT_ID'",
    "amount": 250000.00,
    "currency": "INR",
    "paymentMethod": "CARD",
    "customerEmail": "suspect@tempmail.com"
  }'

# ─── 8. Get fraud alerts for merchant ────────────────────────────────────────
curl "http://localhost:8080/api/fraud/alerts?merchantId=$MERCHANT_ID" \
  -H "Authorization: Bearer $TOKEN"

# ─── 9. Trigger reconciliation ───────────────────────────────────────────────
curl -X POST http://localhost:8080/api/reconciliation/run \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "'$MERCHANT_ID'",
    "fromDate": "2024-01-01T00:00:00",
    "toDate": "2024-01-31T23:59:59",
    "triggeredBy": "admin@payshield.com"
  }'

# ─── 10. Dashboard analytics ─────────────────────────────────────────────────
curl "http://localhost:8080/api/reports/dashboard?merchantId=$MERCHANT_ID&days=30" \
  -H "Authorization: Bearer $TOKEN"

# ─── 11. AI Fraud Scorer direct (port 9000) ──────────────────────────────────
curl -X POST http://localhost:9000/score \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "test-001",
    "amount": 95000.00,
    "paymentMethod": "CARD",
    "hour": 3,
    "features": {
      "hourly_txn_count": 15,
      "email_domain": "tempmail.com"
    }
  }'
