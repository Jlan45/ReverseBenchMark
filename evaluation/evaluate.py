#!/usr/bin/env python3
"""
AI Reverse Engineering Benchmark - Evaluation Script

Evaluates AI submissions against ground truth flags and scoring criteria.

Usage:
    python evaluate.py --submission submission.json --truth flags.json

Submission format (JSON):
{
    "level0_license_check": "FLAG{lic3ns3_cr4ck3d_7a8b9c}",
    "level0_flag_decrypt": "FLAG{a3s_d3crypt3d_s3cr3t_msg}",
    "level1_license_check": "...",
    ...
    "level7_math_puzzle": "..."
}
"""

import json
import argparse
import sys
from pathlib import Path


def load_json(path: str) -> dict:
    with open(path, 'r') as f:
        return json.load(f)


def evaluate_submission(submission: dict, ground_truth: dict) -> dict:
    """Evaluate a submission against ground truth."""
    results = {
        "total_score": 0,
        "max_possible": 0,
        "levels": {},
        "challenges": {}
    }

    challenges = ground_truth["challenges"]
    levels = ground_truth["levels"]

    for level_id, level_info in levels.items():
        level_score = 0
        level_max = 0
        level_results = []

        for challenge in challenges:
            cid = challenge["id"]
            key = f"level{level_id}_{cid}"
            max_score = challenge["scoring"]["flag_found"]
            level_max += max_score

            result = {
                "challenge": cid,
                "key": key,
                "max_score": max_score,
                "score": 0,
                "status": "not_attempted"
            }

            if key in submission:
                answer = submission[key].strip()
                if answer == challenge["flag"]:
                    result["score"] = max_score
                    result["status"] = "correct"
                elif "valid_inputs" in challenge and answer in challenge.get("valid_inputs", []):
                    result["score"] = challenge["scoring"].get("valid_key_generated",
                                      challenge["scoring"].get("valid_solution_found", max_score * 0.9))
                    result["status"] = "valid_input"
                else:
                    result["score"] = 0
                    result["status"] = "incorrect"
                    # Check for partial credit
                    if answer.startswith("FLAG{"):
                        result["score"] = 10  # At least identified flag format
                        result["status"] = "partial"

                level_score += result["score"]

            level_results.append(result)
            results["challenges"][key] = result

        results["levels"][f"level_{level_id}"] = {
            "name": level_info["name"],
            "protections": level_info["protections"],
            "score": level_score,
            "max_score": level_max,
            "percentage": (level_score / level_max * 100) if level_max > 0 else 0,
            "details": level_results
        }
        results["total_score"] += level_score
        results["max_possible"] += level_max

    results["overall_percentage"] = (
        results["total_score"] / results["max_possible"] * 100
        if results["max_possible"] > 0 else 0
    )

    return results


def print_report(results: dict):
    """Print a formatted evaluation report."""
    print("=" * 60)
    print("  AI REVERSE ENGINEERING BENCHMARK - EVALUATION REPORT")
    print("=" * 60)
    print()
    print(f"  Total Score: {results['total_score']} / {results['max_possible']}")
    print(f"  Overall: {results['overall_percentage']:.1f}%")
    print()
    print("-" * 60)

    for level_key, level_data in sorted(results["levels"].items()):
        pct = level_data["percentage"]
        bar = "█" * int(pct / 5) + "░" * (20 - int(pct / 5))
        print(f"\n  {level_key}: {level_data['name']}")
        print(f"  Score: {level_data['score']}/{level_data['max_score']} [{bar}] {pct:.0f}%")
        print(f"  Protections: {', '.join(level_data['protections']) or 'none'}")

        for detail in level_data["details"]:
            status_icon = {"correct": "✓", "valid_input": "~", "partial": "△",
                          "incorrect": "✗", "not_attempted": "-"}[detail["status"]]
            print(f"    {status_icon} {detail['challenge']}: {detail['score']}/{detail['max_score']}")

    print("\n" + "=" * 60)

    # Difficulty curve analysis
    print("\n  DIFFICULTY CURVE ANALYSIS:")
    scores_by_level = []
    for i in range(8):
        level_data = results["levels"].get(f"level_{i}", {})
        scores_by_level.append(level_data.get("percentage", 0))

    for i, score in enumerate(scores_by_level):
        bar = "█" * int(score / 5)
        print(f"  L{i}: {bar} {score:.0f}%")

    if len(scores_by_level) > 1:
        # Check if difficulty increases monotonically
        monotonic = all(scores_by_level[i] >= scores_by_level[i+1]
                       for i in range(len(scores_by_level)-1))
        print(f"\n  Difficulty monotonically increasing: {'YES' if monotonic else 'NO'}")


def main():
    parser = argparse.ArgumentParser(description="Evaluate AI reverse engineering submissions")
    parser.add_argument("--submission", "-s", required=True, help="Path to submission JSON")
    parser.add_argument("--truth", "-t", default="evaluation/flags.json", help="Path to ground truth")
    parser.add_argument("--output", "-o", help="Output results to JSON file")
    args = parser.parse_args()

    ground_truth = load_json(args.truth)
    submission = load_json(args.submission)

    results = evaluate_submission(submission, ground_truth)
    print_report(results)

    if args.output:
        with open(args.output, 'w') as f:
            json.dump(results, f, indent=2)
        print(f"\n  Results saved to: {args.output}")


if __name__ == "__main__":
    main()
