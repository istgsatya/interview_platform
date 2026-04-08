# main.py — Interview ML Engine v2.0
# Requires: rf_model.pkl (run train_model.py first)
# Port    : 8000

from fastapi import FastAPI
from pydantic import BaseModel
import numpy as np
import pickle
import re
import os
import json
import urllib.request
import urllib.error
from sentence_transformers import SentenceTransformer, util

app = FastAPI(title="Interview ML Engine", version="2.0")

# ── Load models once at startup ───────────────────────────────────────────────
print("Loading SentenceTransformer...")
embedder = SentenceTransformer("all-MiniLM-L6-v2")
print("✅ SentenceTransformer ready.")

RF_MODEL_PATH = "rf_model.pkl"
if not os.path.exists(RF_MODEL_PATH):
    raise RuntimeError("rf_model.pkl not found. Run: python train_model.py first.")

with open(RF_MODEL_PATH, "rb") as f:
    rf_model = pickle.load(f)
print("✅ Random Forest model loaded.")


# ── Schemas ───────────────────────────────────────────────────────────────────
class AnswerPayload(BaseModel):
    candidate_answer: str
    ideal_answer: str
    user_context: str = ""

class EvaluationResult(BaseModel):
    score: float
    similarity_score: float
    length_ratio: float
    flags: list[str]


# ── Feature Extraction Helpers ────────────────────────────────────────────────
STOPWORDS = {
    "the", "a", "an", "is", "it", "in", "on", "at", "to", "for",
    "of", "and", "or", "with", "this", "that", "are", "was", "be",
    "can", "will", "you", "we", "they", "i", "have", "has", "from",
    "by", "as", "if", "its", "but", "not", "so", "do", "does", "how",
    "what", "when", "which", "used", "use", "using", "uses", "also",
    "their", "there", "than", "then", "more", "some", "into", "would"
}

def extract_keywords(text: str) -> set[str]:
    words = re.findall(r'\b[a-zA-Z][\w\-\.]*\b', text.lower())
    return {w for w in words if len(w) > 3 and w not in STOPWORDS}

def compute_keyword_coverage(candidate: str, ideal: str) -> float:
    ideal_kw  = extract_keywords(ideal)
    cand_kw   = extract_keywords(candidate)
    if not ideal_kw:
        return 1.0
    return len(ideal_kw & cand_kw) / len(ideal_kw)

def compute_length_ratio(candidate: str, ideal: str) -> float:
    cand_len  = len(candidate.split())
    ideal_len = len(ideal.split())
    if ideal_len == 0:
        return 0.0
    return min(cand_len / ideal_len, 2.0)  # cap at 2.0

def compute_avg_word_complexity(text: str) -> float:
    words = text.split()
    if not words:
        return 0.0
    return min(np.mean([len(w) for w in words]) / 10.0, 1.0)

def find_missing_keywords(candidate: str, ideal: str, top_n: int = 3) -> list[str]:
    missing = extract_keywords(ideal) - extract_keywords(candidate)
    return sorted(missing, key=len, reverse=True)[:top_n]


def clamp(value: float, low: float, high: float) -> float:
    return max(low, min(high, value))


def extract_json_object(text: str) -> dict:
    cleaned = text.strip().replace("```json", "").replace("```", "").strip()

    try:
        return json.loads(cleaned)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", cleaned, flags=re.DOTALL)
        if not match:
            raise
        return json.loads(match.group(0))


def parse_llm_score_feedback(content: str) -> tuple[float, str]:
    cleaned = content.strip().replace("```json", "").replace("```", "").strip()

    # 1) Preferred path: valid JSON object
    try:
        obj = extract_json_object(cleaned)
        score = clamp(float(obj.get("score", 5.0)), 0.0, 10.0)
        feedback = str(obj.get("feedback", "")).strip()
        if feedback:
            return score, feedback
    except Exception:
        pass

    # 2) Recovery path: regex extraction from partially malformed JSON/text
    score_match = re.search(r'"?score"?\s*[:=]\s*([0-9]+(?:\.[0-9]+)?)', cleaned, flags=re.IGNORECASE)
    score = clamp(float(score_match.group(1)), 0.0, 10.0) if score_match else 5.0

    feedback_match = re.search(r'"?feedback"?\s*[:=]\s*"([^"]+)"', cleaned, flags=re.IGNORECASE)
    if feedback_match:
        feedback = feedback_match.group(1).strip()
    else:
        # If model returned plain text, keep it as actionable analysis text.
        feedback = re.sub(r'\s+', ' ', cleaned).strip()
        if len(feedback) > 240:
            feedback = feedback[:240].rstrip() + "..."

    if not feedback:
        feedback = "Increase technical specificity and align your response more closely to core requirements."

    return score, feedback


def fetch_llm_analysis(user_context: str, ideal_answer: str, candidate_answer: str) -> tuple[float, str]:
    api_key = "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"
    if not api_key:
        raise RuntimeError("Missing GROQ_API_KEY")

    prompt_system = (
        "You evaluate technical interview answers. Return ONLY strict JSON with keys: "
        "score (number 0.0 to 10.0) and feedback (1-2 sentences, actionable, concise)."
    )
    prompt_user = (
        f"Candidate Resume Context:\n{user_context}\n\n"
        f"Reference Ideal Answer:\n{ideal_answer}\n\n"
        f"Candidate Answer:\n{candidate_answer}\n\n"
        "Output format exactly:\n"
        "{\"score\": 7.8, \"feedback\": \"...\"}"
    )

    body = {
        "model": "openai/gpt-oss-120b",
        "temperature": 0.1,
        "max_tokens": 180,
        "messages": [
            {"role": "system", "content": prompt_system},
            {"role": "user", "content": prompt_user},
        ],
    }

    req = urllib.request.Request(
        url="https://api.groq.com/openai/v1/chat/completions",
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
            "User-Agent": "Mozilla/5.0",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(req, timeout=6.0) as response:
            response_payload = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        response_body = ""
        try:
            response_body = e.read().decode("utf-8", errors="replace")
        except Exception:
            response_body = "<unable to decode response body>"
        raise RuntimeError(f"Groq HTTP {e.code}: {response_body}") from e

    content = response_payload["choices"][0]["message"]["content"]
    llm_score, llm_feedback = parse_llm_score_feedback(content)

    if not llm_feedback:
        llm_feedback = "Increase technical specificity and align your response more closely to core requirements."

    return llm_score, llm_feedback


# ── Flag Generator ────────────────────────────────────────────────────────────
def generate_flags(
    score: float,
    similarity: float,
    length_ratio: float,
    kw_coverage: float,
    candidate: str,
    ideal: str
) -> list[str]:
    flags = []
    cand_len = len(candidate.split())

    if cand_len < 15:
        flags.append("Answer is too short — aim for at least 3-4 sentences.")

    if length_ratio > 1.8:
        flags.append("Answer is too verbose — focus on key concepts.")

    if similarity < 0.35:
        flags.append("Answer appears off-topic — re-read the question carefully.")
    elif similarity < 0.55:
        flags.append("Answer partially addresses the question.")

    if kw_coverage < 0.30:
        missing = find_missing_keywords(candidate, ideal, top_n=3)
        if missing:
            flags.append(f"Missing key concepts: {', '.join(missing)}.")

    # Positive feedback if no issues
    if not flags:
        if score >= 7.0:
            flags.append("Strong answer — good conceptual coverage.")
        elif score >= 5.0:
            flags.append("Decent answer — could use more technical depth.")
        else:
            flags.append("Needs improvement — review the core concepts.")

    return flags


# ── Endpoints ─────────────────────────────────────────────────────────────────
@app.get("/")
def health_check():
    return {
        "status"  : "ML Engine v2.0 is live",
        "models"  : ["all-MiniLM-L6-v2", "RandomForestRegressor"],
        "version" : "2.0"
    }


@app.post("/evaluate", response_model=EvaluationResult)
def evaluate_answer(payload: AnswerPayload):
    print(f"\n── Evaluation Request ────────────────────────────")
    print(f"  Ideal     : {payload.ideal_answer[:60]}...")
    print(f"  Candidate : {payload.candidate_answer[:60]}...")

    candidate = payload.candidate_answer.strip()
    ideal     = payload.ideal_answer.strip()

    # ── Feature 1: Semantic Similarity ────────────────────────────────────────
    # SentenceTransformer encodes meaning, not just words.
    # "containers isolate processes" ≈ "Docker sandboxes applications" → high score
    emb_cand   = embedder.encode(candidate, convert_to_tensor=True)
    emb_ideal  = embedder.encode(ideal,     convert_to_tensor=True)
    similarity = float(util.cos_sim(emb_cand, emb_ideal)[0][0])
    similarity = max(0.0, min(1.0, similarity))

    # ── Feature 2: Length Ratio ───────────────────────────────────────────────
    length_ratio = compute_length_ratio(candidate, ideal)

    # ── Feature 3: Keyword Coverage ───────────────────────────────────────────
    kw_coverage = compute_keyword_coverage(candidate, ideal)

    # ── Feature 4: Vocabulary Complexity ─────────────────────────────────────
    complexity = compute_avg_word_complexity(candidate)

    # ── Random Forest Prediction ──────────────────────────────────────────────
    features    = np.array([[similarity, length_ratio, kw_coverage, complexity]])
    raw_rf_score = float(rf_model.predict(features)[0])
    rf_score = round(clamp(raw_rf_score, 0.0, 10.0), 2)

    # ── Hybrid ensemble with resilient fallback ───────────────────────────────
    using_llm = False
    llm_score = None
    llm_feedback = ""

    try:
        llm_score, llm_feedback = fetch_llm_analysis(
            user_context=payload.user_context.strip(),
            ideal_answer=ideal,
            candidate_answer=candidate,
        )
        using_llm = True
        blended = (llm_score * 0.80) + (rf_score * 0.20) + 0.5
        final_score = round(clamp(blended, 0.0, 10.0), 2)
        flags = [f"Analysis: {llm_feedback}"]
    except Exception as e:
        print(f"\n❌ GROQ API FAILED: {str(e)}\n")
        final_score = rf_score
        flags = generate_flags(
            final_score, similarity, length_ratio, kw_coverage, candidate, ideal
        )

    print(f"  similarity={similarity:.3f} | length={length_ratio:.2f} "
          f"| keywords={kw_coverage:.2f} | complexity={complexity:.2f}")
    if using_llm:
        print(f"  Ensemble: rf={rf_score:.2f}, llm={llm_score:.2f}, final={final_score:.2f}")
    else:
        print(f"  Score: {final_score}/10 | Flags: {flags}")

    return EvaluationResult(
        score=final_score,
        similarity_score=round(similarity, 4),
        length_ratio=round(length_ratio, 4),
        flags=flags
    )
