#!/bin/bash
set -e
BASE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Starting PayShield Platform services..."
echo ""

# Start each service in background
for svc in payshield-auth payshield-payment payshield-fraud \
           payshield-reconciliation payshield-notification payshield-reporting payshield-gateway; do
  echo "Starting $svc..."
  cd "$BASE/$svc"
  mvn spring-boot:run -q &
  sleep 2
done

echo ""
echo "Starting AI Fraud Scorer..."
cd "$BASE/payshield-ai-scorer"
uvicorn app.main:app --host 0.0.0.0 --port 9000 &

echo ""
echo "All services started!"
echo ""
echo "  Gateway:        http://localhost:8080"
echo "  Auth:           http://localhost:8081"
echo "  Payments:       http://localhost:8082"
echo "  Fraud:          http://localhost:8083"
echo "  Reconciliation: http://localhost:8084"
echo "  Notifications:  http://localhost:8085"
echo "  Reporting:      http://localhost:8086"
echo "  AI Scorer:      http://localhost:9000"
echo "  Frontend:       open frontend/index.html in browser"
echo ""
echo "Press Ctrl+C to stop all services"
wait
