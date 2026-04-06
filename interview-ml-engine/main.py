from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import numpy as np
# We will import the ML models here later

app = FastAPI(title="Interview ML Engine", version="1.0")

# 1. Define what the Java backend will send us
class AnswerPayload(BaseModel):
    candidate_answer: str
    ideal_answer: str

# 2. Define what we will send back to Java
class EvaluationResult(BaseModel):
    score: float
    similarity_score: float
    length_ratio: float
    flags: list[str]

@app.get("/")
def health_check():
    return {"status": "ML Engine is running securely"}

@app.post("/evaluate", response_model=EvaluationResult)
def evaluate_answer(payload: AnswerPayload):
    print(f"--- Received Evaluation Request ---")
    print(f"Ideal: {payload.ideal_answer[:50]}...")
    print(f"Candidate: {payload.candidate_answer[:50]}...")

    # TODO: This is where we will insert your actual SentenceTransformer 
    # and Random Forest logic. For right now, we calculate basic metrics 
    # to test the Java -> Python bridge.
    
    cand_len = len(payload.candidate_answer.split())
    ideal_len = len(payload.ideal_answer.split())
    
    # Calculate a rough length ratio
    length_ratio = cand_len / ideal_len if ideal_len > 0 else 0
    
    # Dummy similarity score for architectural testing
    sim_score = 0.75 
    
    # Dummy Random forest calculation
    final_score = (sim_score * 0.7) + (min(length_ratio, 1.0) * 0.3) * 10

    flags = []
    if length_ratio < 0.4:
        flags.append("Answer is too short compared to the ideal baseline.")
    if "docker" in payload.ideal_answer.lower() and "docker" not in payload.candidate_answer.lower():
        flags.append("Missing critical keyword: Docker")

    print(f"Calculated Score: {round(final_score, 2)}/10")

    return EvaluationResult(
        score=round(final_score, 2),
        similarity_score=sim_score,
        length_ratio=length_ratio,
        flags=flags
    )

if __name__ == "__main__":
    import uvicorn
    # Runs the Python ML server on port 8000
    uvicorn.run(app, host="0.0.0.0", port=8000)
