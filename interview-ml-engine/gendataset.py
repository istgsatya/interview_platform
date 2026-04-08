import random
import json

def clamp(x, lo, hi):
    return max(lo, min(hi, x))

def generate_sample():
    semantic = random.uniform(0, 1)
    keyword = random.uniform(0, 1)
    length = random.uniform(0, 2)
    complexity = random.uniform(0, 1)

    mode = random.random()

    # -------------------------------
    # MODE 1: CLEAN SIGNAL (60%)
    # -------------------------------
    if mode < 0.6:
        score = (
            semantic * 0.65 +
            keyword * 0.25 +
            complexity * 0.1
        ) * 10

        # small noise
        score += random.uniform(-0.3, 0.3)
    # Very strong top-tier samples (small %)
    if random.random() < 0.08:
        semantic = random.uniform(0.92, 1.0)
        keyword = random.uniform(0.9, 1.0)
        length = random.uniform(0.9, 1.1)
        complexity = random.uniform(0.7, 1.0)
        score = random.uniform(8.5, 10.0) 
    # -------------------------------
    # MODE 2: MILD NOISE (30%)
    # -------------------------------
    elif mode < 0.9:
        score = (
            semantic * random.uniform(0.5, 0.7) +
            keyword * random.uniform(0.2, 0.3) +
            complexity * random.uniform(0.05, 0.15)
        ) * 10

        # moderate noise
        score += random.uniform(-0.8, 0.8)

        # occasional bias
        if random.random() < 0.2:
            score += random.uniform(-1, 1)

    # -------------------------------
    # MODE 3: EDGE CASES (10%)
    # -------------------------------
    else:
        case = random.choice([
            "short_good", "long_bad", "keyword_spam", "fluent_wrong"
        ])

        if case == "short_good":
            semantic = random.uniform(0.85, 1.0)
            length = random.uniform(0.1, 0.4)
            keyword = random.uniform(0.6, 0.9)
            score = random.uniform(7.5, 9.5)

        elif case == "long_bad":
            semantic = random.uniform(0.0, 0.4)
            length = random.uniform(1.6, 2.0)
            score = random.uniform(1.0, 4.0)

        elif case == "keyword_spam":
            keyword = random.uniform(0.9, 1.0)
            semantic = random.uniform(0.2, 0.5)
            score = random.uniform(3.0, 6.0)

        else:  # fluent_wrong
            complexity = random.uniform(0.8, 1.0)
            semantic = random.uniform(0.0, 0.3)
            score = random.uniform(1.0, 4.0)

    # -------------------------------
    # HIGH QUALITY BOOST
    # -------------------------------
    if random.random() < 0.15:
        semantic = random.uniform(0.9, 1.0)
        keyword = random.uniform(0.85, 1.0)
        score += random.uniform(1.5, 3.0)

    # -------------------------------
    # LOW QUALITY BOOST
    # -------------------------------
    if random.random() < 0.1:
        semantic = random.uniform(0.0, 0.2)
        keyword = random.uniform(0.0, 0.3)
        score -= random.uniform(1.5, 3.0)

    score = clamp(score, 0, 10)

    return {
        "semantic_similarity": round(semantic, 3),
        "length_ratio": round(length, 3),
        "keyword_coverage": round(keyword, 3),
        "avg_word_complexity": round(complexity, 3),
        "score": round(score, 2)
    }


def generate_dataset(n=3000):
    data = [generate_sample() for _ in range(n)]
    random.shuffle(data)
    return data


if __name__ == "__main__":
    dataset = generate_dataset(3000)

    with open("dataset.json", "w") as f:
        json.dump(dataset, f, indent=2)

    print("✅ Generated dataset with balanced signal + noise")
