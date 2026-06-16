package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"os"
	"strings"

	"github.com/anthropic/reversebenchmark/agent/config"
	"github.com/anthropic/reversebenchmark/agent/pipeline"
)

// GroundTruth matches the JSON structure from buildAllApks
type GroundTruth struct {
	Description string                       `json:"description"`
	Challenges  map[string]ChallengeAnswer   `json:"challenges"`
}

type ChallengeAnswer struct {
	Answer string `json:"answer"`
	Flag   string `json:"flag"`
	Type   string `json:"type"`
}

func main() {
	cfg := config.Load()

	flag.StringVar(&cfg.APKDir, "apk-dir", cfg.APKDir, "Directory containing benchmark APKs")
	flag.IntVar(&cfg.Level, "level", -1, "Run only this level (-1 = all)")
	flag.StringVar(&cfg.OutputDir, "output", cfg.OutputDir, "Output directory for results")
	flag.StringVar(&cfg.GroundTruthFile, "ground-truth", cfg.GroundTruthFile, "Path to ground_truth.json")
	flag.IntVar(&cfg.MaxSteps, "max-steps", cfg.MaxSteps, "Max ReAct agent steps per level")
	flag.BoolVar(&cfg.Verbose, "verbose", false, "Enable verbose tool call logging")
	flag.Parse()

	if cfg.LLMAPIKey == "" {
		fmt.Fprintln(os.Stderr, "Error: LLM_API_KEY environment variable is required")
		os.Exit(1)
	}
	if cfg.IDADir == "" {
		fmt.Fprintln(os.Stderr, "Error: IDADIR environment variable is required")
		os.Exit(1)
	}

	// Default ground truth path
	if cfg.GroundTruthFile == "" {
		cfg.GroundTruthFile = cfg.APKDir + "/ground_truth.json"
	}

	log.Printf("Benchmark Agent starting")
	log.Printf("  LLM: %s/%s", cfg.LLMType, cfg.LLMModel)
	log.Printf("  IDA: %s", cfg.IDADir)
	log.Printf("  APKs: %s", cfg.APKDir)
	log.Printf("  Ground truth: %s", cfg.GroundTruthFile)
	log.Printf("  Max steps: %d", cfg.MaxSteps)

	p := pipeline.New(cfg)
	results, err := p.Run()
	if err != nil {
		log.Fatalf("Pipeline failed: %v", err)
	}

	// Load ground truth
	gt, err := loadGroundTruth(cfg.GroundTruthFile)
	if err != nil {
		log.Printf("Warning: could not load ground truth: %v (skipping scoring)", err)
		printResultsOnly(results)
		return
	}

	// Score and print
	fmt.Println("\n=== Benchmark Results ===")
	totalCorrect := 0
	totalChallenges := 0

	for _, r := range results {
		fmt.Printf("\n  Level %d [%d steps, %v]:\n", r.Level, r.Steps, r.Duration.Round(1000000000))
		if r.Error != "" {
			fmt.Printf("    ERROR: %s\n", r.Error)
		}

		for challengeID, expected := range gt.Challenges {
			totalChallenges++
			submitted, ok := r.Answers[challengeID]
			if !ok {
				fmt.Printf("    [MISS] %s: not attempted\n", challengeID)
				continue
			}
			correct := matchAnswer(submitted, expected)
			if correct {
				totalCorrect++
				fmt.Printf("    [PASS] %s\n", challengeID)
			} else {
				fmt.Printf("    [FAIL] %s: got %q, want %q\n", challengeID, truncate(submitted, 50), truncate(expected.Answer, 50))
			}
		}
	}

	pct := float64(0)
	if totalChallenges > 0 {
		pct = float64(totalCorrect) / float64(totalChallenges) * 100
	}
	fmt.Printf("\n  Score: %d/%d (%.1f%%)\n", totalCorrect, totalChallenges, pct)
	fmt.Printf("  Results saved to: %s/answers.json\n", cfg.OutputDir)
}

func loadGroundTruth(path string) (*GroundTruth, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	var gt GroundTruth
	if err := json.Unmarshal(data, &gt); err != nil {
		return nil, err
	}
	return &gt, nil
}

func matchAnswer(submitted string, expected ChallengeAnswer) bool {
	s := strings.TrimSpace(submitted)
	// Match against answer or flag (either is accepted)
	if strings.EqualFold(s, strings.TrimSpace(expected.Answer)) {
		return true
	}
	if strings.EqualFold(s, strings.TrimSpace(expected.Flag)) {
		return true
	}
	// For integer type, normalize
	if expected.Type == "integer" {
		return strings.TrimSpace(s) == strings.TrimSpace(expected.Answer)
	}
	return false
}

func truncate(s string, max int) string {
	if len(s) > max {
		return s[:max] + "..."
	}
	return s
}

func printResultsOnly(results []pipeline.LevelResult) {
	fmt.Println("\n=== Benchmark Results (no scoring) ===")
	for _, r := range results {
		status := "OK"
		if r.Error != "" {
			status = "ERR"
		}
		fmt.Printf("  Level %d [%s]: %d answers in %d steps (%v)\n",
			r.Level, status, len(r.Answers), r.Steps, r.Duration.Round(1000000000))
		for id, ans := range r.Answers {
			fmt.Printf("    %s = %s\n", id, ans)
		}
	}
}
