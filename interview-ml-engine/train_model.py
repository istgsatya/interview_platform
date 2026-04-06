# train_model.py
# Run once: python train_model.py
# Requires: dataset.json in the same folder
# Output  : rf_model.pkl (loaded by main.py at runtime)

import json
import pickle
import numpy as np
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import mean_absolute_error, r2_score
import os

# ── 1. Load Dataset ───────────────────────────────────────────────────────────
DATASET_PATH = "dataset.json"
MODEL_OUTPUT  = "rf_model.pkl"

if not os.path.exists(DATASET_PATH):
    raise FileNotFoundError(
        f"'{DATASET_PATH}' not found.\n"
        f"Generate it with the other AI first, then place it here."
    )

print(f"Loading dataset from {DATASET_PATH}...")

with open(DATASET_PATH, "r") as f:
    raw = json.load(f)

print(f"Loaded {len(raw)} samples.")

# ── 2. Validate & Extract Features ───────────────────────────────────────────
REQUIRED_KEYS = {"semantic_similarity", "length_ratio", 
                 "keyword_coverage", "avg_word_complexity", "score"}

errors = []
X_list = []
y_list = []

for i, row in enumerate(raw):
    # Check all keys exist
    missing = REQUIRED_KEYS - row.keys()
    if missing:
        errors.append(f"Row {i}: missing keys {missing}")
        continue

    # Check value ranges
    if not (0.0 <= row["semantic_similarity"] <= 1.0):
        errors.append(f"Row {i}: semantic_similarity {row['semantic_similarity']} out of range [0,1]")
    if not (0.0 <= row["length_ratio"] <= 2.0):
        errors.append(f"Row {i}: length_ratio {row['length_ratio']} out of range [0,2]")
    if not (0.0 <= row["keyword_coverage"] <= 1.0):
        errors.append(f"Row {i}: keyword_coverage {row['keyword_coverage']} out of range [0,1]")
    if not (0.0 <= row["avg_word_complexity"] <= 1.0):
        errors.append(f"Row {i}: avg_word_complexity {row['avg_word_complexity']} out of range [0,1]")
    if not (0.0 <= row["score"] <= 10.0):
        errors.append(f"Row {i}: score {row['score']} out of range [0,10]")

    X_list.append([
        row["semantic_similarity"],
        row["length_ratio"],
        row["keyword_coverage"],
        row["avg_word_complexity"]
    ])
    y_list.append(row["score"])

if errors:
    print(f"\n⚠️  Found {len(errors)} validation issues:")
    for e in errors[:10]:  # show first 10 only
        print(f"   {e}")
    if len(errors) > 10:
        print(f"   ... and {len(errors) - 10} more.")
    print("\nFix these in dataset.json before training.\n")
    # Only abort if more than 10% of data is bad
    if len(errors) > len(raw) * 0.1:
        raise ValueError("Too many bad rows — fix dataset before training.")
    print("Continuing with valid rows only...\n")

X = np.array(X_list)
y = np.array(y_list)

print(f"Clean samples ready: {len(X)}")
print(f"Score distribution:")
print(f"   0.0 – 2.4  : {np.sum(y <= 2.4)} samples")
print(f"   2.5 – 4.9  : {np.sum((y > 2.4) & (y <= 4.9))} samples")
print(f"   5.0 – 7.4  : {np.sum((y > 4.9) & (y <= 7.4))} samples")
print(f"   7.5 – 8.9  : {np.sum((y > 7.4) & (y <= 8.9))} samples")
print(f"   9.0 – 10.0 : {np.sum(y >= 9.0)} samples")

# ── 3. Train / Test Split ─────────────────────────────────────────────────────
# 80% train, 20% test — standard split
X_train, X_test, y_train, y_test = train_test_split(
    X, y,
    test_size=0.2,
    random_state=42
)
print(f"\nSplit: {len(X_train)} train / {len(X_test)} test")

# ── 4. Train Random Forest ────────────────────────────────────────────────────
print("\nTraining Random Forest Regressor...")

model = RandomForestRegressor(
    n_estimators=300,
    max_depth=6,          # was 10 — shallower trees generalize better
    min_samples_split=10, # was 4  — needs more samples to split
    min_samples_leaf=5,   # was 2  — bigger leaves = less memorization
    max_features="sqrt",
    random_state=42,
    n_jobs=-1
)
model.fit(X_train, y_train)
print("Training complete.")

# ── 5. Evaluate ───────────────────────────────────────────────────────────────
y_pred = model.predict(X_test)

mae = mean_absolute_error(y_test, y_pred)
r2  = r2_score(y_test, y_pred)

# Cross validation on full dataset — more reliable than single split
cv_scores = cross_val_score(model, X, y, cv=5, scoring="r2")

print(f"\n── Model Evaluation ──────────────────────────────")
print(f"   MAE  (mean absolute error) : {mae:.4f}")
print(f"   R²   (test set)            : {r2:.4f}")
print(f"   R²   (5-fold cross-val)    : {cv_scores.mean():.4f} ± {cv_scores.std():.4f}")

# ── 6. Feature Importance ─────────────────────────────────────────────────────
feature_names = [
    "semantic_similarity",
    "length_ratio",
    "keyword_coverage",
    "avg_word_complexity"
]
importances = model.feature_importances_

print(f"\n── Feature Importances ───────────────────────────")
for name, imp in sorted(zip(feature_names, importances), 
                         key=lambda x: x[1], reverse=True):
    bar = "█" * int(imp * 40)
    print(f"   {name:<25} {imp:.4f}  {bar}")

# ── 7. Sanity Checks ──────────────────────────────────────────────────────────
print(f"\n── Sanity Checks ─────────────────────────────────")
sanity_cases = [
    ([0.92, 1.0,  1.0,  0.6],  "Perfect answer         "),
    ([0.80, 0.9,  0.75, 0.5],  "Strong answer          "),
    ([0.60, 0.8,  0.55, 0.4],  "Average answer         "),
    ([0.35, 0.4,  0.25, 0.2],  "Weak answer            "),
    ([0.10, 0.1,  0.0,  0.05], "Poor / blank answer    "),
    ([0.85, 0.25, 0.80, 0.6],  "Knows topic, too short "),
    ([0.30, 1.8,  0.20, 0.2],  "Rambling, off-topic    "),
]

for features, label in sanity_cases:
    pred = model.predict(np.array([features]))[0]
    pred = max(0.0, min(10.0, pred))
    bar  = "█" * int(pred)
    print(f"   {label}: {pred:.2f}/10  {bar}")

# ── 8. Save Model ─────────────────────────────────────────────────────────────
with open(MODEL_OUTPUT, "wb") as f:
    pickle.dump(model, f)

print(f"\n✅ Model saved to {MODEL_OUTPUT}")
print(f"   Start your ML server: python main.py")
