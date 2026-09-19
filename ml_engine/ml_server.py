from flask import Flask, request, jsonify
import random

# Initialize the Flask app
app = Flask(__name__)

# This endpoint matches the URL in MLService.java (http://127.0.0.1:8000/analyze)
@app.route('/analyze', methods=['POST'])
def analyze_project():
    data = request.json
    
    print(f"--- Received Request from Java ---")
    print(f"Analyzing Project ID: {data.get('project_id')}")
    print(f"Amount: ₹{data.get('allocation_amount')}")
    print(f"Location: {data.get('district')}, {data.get('state')}")
    
    # =================================================================
    # THIS IS WHERE YOUR ACTUAL MACHINE LEARNING CODE WILL GO!
    # E.g., loading a pandas dataframe, running a scikit-learn model, etc.
    # df = pd.DataFrame([data])
    # prediction = my_model.predict(df)
    # =================================================================

    # For now, we simulate an AI prediction engine
    allocation = float(data.get('allocation_amount', 0))
    status = data.get('status', '')
    
    base_risk = 0.2
    if allocation > 2000000: # 20 Lakhs
        base_risk += 0.4
    if status == "Delayed":
        base_risk += 0.3
        
    # Add a little randomness to the AI anomaly score
    anomaly_score = min(0.99, base_risk + random.uniform(-0.1, 0.1))
    risk_score = round(anomaly_score * 100, 1)
    
    if risk_score >= 70:
        risk_level = "High"
        recommendation = "Immediate audit required. Funds at risk of misuse or severe delay."
    elif risk_score >= 40:
        risk_level = "Medium"
        recommendation = "Monitor closely. Minor anomalies detected in allocation size."
    else:
        risk_level = "Low"
        recommendation = "Project looks normal. No anomalies detected."

    print(f"Result: {risk_level} Risk (Score: {risk_score})\n")

    # Return the JSON response back to Java!
    return jsonify({
        "risk_score": risk_score,
        "risk_level": risk_level,
        "anomaly_score": round(anomaly_score, 3),
        "recommendation": recommendation,
        "model_version": "v1.0"
    })

if __name__ == '__main__':
    # Run the server on port 8000
    app.run(host='127.0.0.1', port=8000, debug=True)
