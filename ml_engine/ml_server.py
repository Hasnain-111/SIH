import os
import json
import logging
from datetime import datetime

import joblib
import numpy as np
import pandas as pd
from flask import Flask, request, jsonify

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("ml_server")

app = Flask(__name__)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "MPLADS_IsolationForest_Model.pkl")
STATS_PATH = os.path.join(BASE_DIR, "preprocessing_stats.json")   # optional, see notes
MODEL_VERSION = "IsolationForest-v1"

# =====================================================================
# Load the model ONCE at startup
# =====================================================================
model = joblib.load(MODEL_PATH)
FEATURE_COLUMNS = list(model.feature_names_in_)      # the exact 58 columns, in order
log.info("Model loaded: %s with %d features", type(model).__name__, len(FEATURE_COLUMNS))

# =====================================================================
# Values the model needs but Java does not send (neutral defaults).
# Optional: preprocessing_stats.json can supply "state_frequency".
# =====================================================================
DEFAULTS = {
    "state_frequency": 0.03,
    "constituency_frequency": 0.001,
    "work_frequency": 0.001,
    "work_text_length": 60,
    "work_word_count": 10,
    "category": "Normal/Others",
    "house": "Lok Sabha",
}
STATS = {}
if os.path.exists(STATS_PATH):
    with open(STATS_PATH, "r", encoding="utf-8") as f:
        STATS = json.load(f)
    log.info("Loaded preprocessing stats from %s", STATS_PATH)

# Java status values -> statuses the model was trained on
STATUS_ALIASES = {
    "delayed": "Ongoing",
    "in progress": "Ongoing",
    "pending": "Sanctioned",
    "not started": "Sanctioned",
}

# Scale for converting the model's decision_function to 0-100 risk.
# decision_function < 0 means "anomaly"; typical range is about -0.10 .. +0.05
DECISION_SCALE = 0.06


def _to_float(value, default=0.0):
    try:
        if value is None or value == "":
            return default
        return float(value)
    except (TypeError, ValueError):
        return default


def _set_onehot(row: dict, prefix: str, value):
    """Set PREFIX_value = 1 if that column exists (case-insensitive).
    Unknown values leave all columns 0, which is how the model saw
    rare/missing categories."""
    if value is None:
        return
    target = f"{prefix}_{str(value).strip()}".lower()
    for col in FEATURE_COLUMNS:
        if col.lower() == target:
            row[col] = 1.0
            return


def build_features(data: dict) -> pd.DataFrame:
    row = dict.fromkeys(FEATURE_COLUMNS, 0.0)

    alloc = max(_to_float(data.get("allocation_amount")), 0.0)
    state = (data.get("state") or "").strip()
    status = (data.get("status") or "").strip()
    status = STATUS_ALIASES.get(status.lower(), status)

    # Date features (use recommended_date if Java sends it, else today)
    dt = datetime.now()
    raw_date = data.get("recommended_date")
    if raw_date:
        try:
            dt = pd.to_datetime(raw_date).to_pydatetime()
        except Exception:
            pass

    work_text = str(data.get("work_name") or data.get("description") or "")
    text_len = len(work_text) if work_text else DEFAULTS["work_text_length"]
    word_cnt = len(work_text.split()) if work_text else DEFAULTS["work_word_count"]

    row.update({
        "ALLOCATION AMOUNT": alloc,
        "LOG_ALLOCATION": float(np.log1p(alloc)),
        "ZERO_ALLOCATION_FLAG": 1.0 if alloc == 0 else 0.0,
        # Peer-median is not estimated. The model requires these two columns,
        # so they are set to a neutral value: the project equals its own
        # "peer median" (ratio = 1), which adds no peer-based signal.
        "PEER_MEDIAN_ALLOCATION": alloc,
        "PEER_ALLOCATION_RATIO": 1.0,
        "RECOMMENDED_YEAR": float(dt.year),
        "RECOMMENDED_MONTH": float(dt.month),
        "RECOMMENDED_QUARTER": float((dt.month - 1) // 3 + 1),
        "RECOMMENDED_DAY_OF_WEEK": float(dt.weekday()),
        "WORK_TEXT_LENGTH": float(text_len),
        "WORK_WORD_COUNT": float(word_cnt),
        "STATE_FREQUENCY": _to_float(
            STATS.get("state_frequency", {}).get(state), DEFAULTS["state_frequency"]),
        "CONSTITUENCY_FREQUENCY": _to_float(
            data.get("constituency_frequency"), DEFAULTS["constituency_frequency"]),
        "WORK_FREQUENCY": _to_float(
            data.get("work_frequency"), DEFAULTS["work_frequency"]),
    })

    _set_onehot(row, "STATE", state)
    _set_onehot(row, "STATUS", status)
    _set_onehot(row, "CATEGORY", data.get("category") or DEFAULTS["category"])
    _set_onehot(row, "HOUSE", data.get("house") or DEFAULTS["house"])

    # exact column order the model was trained with
    return pd.DataFrame([row], columns=FEATURE_COLUMNS)


def score(df: pd.DataFrame):
    """Returns (anomaly_score 0..1, risk_score 0..100)."""
    decision = float(model.decision_function(df)[0])       # <0 => anomaly
    anomaly_score = float(np.clip(-model.score_samples(df)[0], 0.0, 1.0))
    risk = 50.0 - 50.0 * (decision / DECISION_SCALE)
    risk_score = round(float(np.clip(risk, 0.0, 100.0)), 1)
    return anomaly_score, risk_score


def interpret(risk_score: float):
    if risk_score >= 70:
        return "High", "Immediate audit required. Funds at risk of misuse or severe delay."
    if risk_score >= 40:
        return "Medium", "Monitor closely. Minor anomalies detected in allocation size."
    return "Low", "Project looks normal. No anomalies detected."


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model": MODEL_VERSION})


# This endpoint matches the URL in MLService.java (http://127.0.0.1:8000/analyze)
@app.route("/analyze", methods=["POST"])
def analyze_project():
    data = request.get_json(silent=True)
    if not isinstance(data, dict):
        return jsonify({"error": "Request body must be a JSON object"}), 400

    log.info("Analyzing project %s | Rs.%s | %s, %s | %s",
             data.get("project_id"), data.get("allocation_amount"),
             data.get("district"), data.get("state"), data.get("status"))

    try:
        features = build_features(data)
        anomaly_score, risk_score = score(features)
    except Exception as e:
        log.exception("Prediction failed")
        return jsonify({"error": f"Prediction failed: {e}"}), 500

    risk_level, recommendation = interpret(risk_score)
    log.info("Result: %s risk (score %s)", risk_level, risk_score)

    # Same response shape Java already expects
    return jsonify({
        "risk_score": risk_score,
        "risk_level": risk_level,
        "anomaly_score": round(anomaly_score, 3),
        "recommendation": recommendation,
        "model_version": MODEL_VERSION,
    })


if __name__ == "__main__":
    # debug=False so the model isn't loaded twice by the reloader
    app.run(host="127.0.0.1", port=8000, debug=False)
