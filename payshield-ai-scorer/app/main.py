from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Optional, Dict, Any
import numpy as np
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="PayShield AI Fraud Scorer",
    description="ML-powered fraud scoring API",
    version="1.0.0"
)


class ScorerRequest(BaseModel):
    transactionId: str
    amount: float
    paymentMethod: str
    hour: int
    features: Optional[Dict[str, Any]] = {}


class ScorerResponse(BaseModel):
    transactionId: str
    score: float
    modelVersion: str
    featureImportance: Dict[str, float]
    riskLevel: str


class FraudModel:
    """
    Simulated XGBoost fraud model.
    In production: load a trained model from disk or MLflow.
    """
    MODEL_VERSION = "v1.2-xgboost"

    def predict(self, request: ScorerRequest) -> tuple[float, dict]:
        score = 0.0
        importance = {}

        # Feature 1: Amount scoring (log-scaled)
        amount_score = min(np.log1p(request.amount) / np.log1p(500000) * 40, 40)
        score += amount_score
        importance["amount"] = round(amount_score / 100, 3)

        # Feature 2: Hour of day (risk peaks at night)
        hour = request.hour
        if 0 <= hour <= 5:
            hour_score = 25.0
        elif 22 <= hour <= 23:
            hour_score = 15.0
        else:
            hour_score = 0.0
        score += hour_score
        importance["hour_of_day"] = round(hour_score / 100, 3)

        # Feature 3: Payment method risk
        method_risk = {
            "CARD": 10.0,
            "UPI": 5.0,
            "NET_BANKING": 8.0,
            "WALLET": 6.0,
            "BANK_TRANSFER": 12.0
        }
        method_score = method_risk.get(request.paymentMethod.upper(), 10.0)
        score += method_score
        importance["payment_method"] = round(method_score / 100, 3)

        # Feature 4: Hourly transaction count velocity (from features dict)
        hourly_count = request.features.get("hourly_txn_count", 0)
        if isinstance(hourly_count, (int, float)) and hourly_count > 5:
            velocity_score = min((hourly_count - 5) * 3.0, 25.0)
            score += velocity_score
            importance["velocity"] = round(velocity_score / 100, 3)
        else:
            importance["velocity"] = 0.0

        # Feature 5: Email domain risk
        if request.features.get("email_domain"):
            disposable_domains = {"mailinator.com", "tempmail.com",
                                  "throwaway.email", "guerrillamail.com"}
            if request.features["email_domain"] in disposable_domains:
                score += 20.0
                importance["email_risk"] = 0.20
            else:
                importance["email_risk"] = 0.0

        # Add controlled randomness to simulate real model variance
        score += np.random.normal(0, 2)
        score = max(0.0, min(100.0, score))

        return round(score, 2), importance


model = FraudModel()


@app.post("/score", response_model=ScorerResponse)
async def score_transaction(request: ScorerRequest):
    try:
        logger.info(f"Scoring transaction: {request.transactionId}")
        score, importance = model.predict(request)

        risk_level = (
            "CRITICAL" if score >= 90 else
            "HIGH"     if score >= 75 else
            "MEDIUM"   if score >= 50 else
            "LOW"
        )

        return ScorerResponse(
            transactionId=request.transactionId,
            score=score,
            modelVersion=FraudModel.MODEL_VERSION,
            featureImportance=importance,
            riskLevel=risk_level
        )
    except Exception as e:
        logger.error(f"Scoring error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/health")
async def health():
    return {"status": "UP", "model": FraudModel.MODEL_VERSION}


@app.get("/model-info")
async def model_info():
    return {
        "version": FraudModel.MODEL_VERSION,
        "type": "XGBoost (simulated)",
        "features": ["amount", "hour_of_day", "payment_method", "velocity", "email_risk"],
        "scoreRange": "0-100",
        "thresholds": {"flag": 75, "reject": 90}
    }
