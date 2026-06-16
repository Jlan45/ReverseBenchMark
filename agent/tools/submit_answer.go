package tools

import (
	"context"
	"encoding/json"
	"fmt"
	"sync"

	"github.com/cloudwego/eino/components/tool"
	"github.com/cloudwego/eino/schema"
)

// SubmitAnswerTool allows the agent to submit challenge answers.
// Answers are collected in the shared Answers map for scoring.
type SubmitAnswerTool struct {
	Answers map[string]string
	mu      sync.Mutex
}

func NewSubmitAnswerTool() *SubmitAnswerTool {
	return &SubmitAnswerTool{
		Answers: make(map[string]string),
	}
}

func (t *SubmitAnswerTool) Info(ctx context.Context) (*schema.ToolInfo, error) {
	return &schema.ToolInfo{
		Name: "submit_answer",
		Desc: `Submit your answer for a reverse engineering challenge. Call this for each challenge you solve.
Valid challenge IDs: license_check, flag_decrypt, algorithm_reversal, serial_gen, math_puzzle`,
		ParamsOneOf: schema.NewParamsOneOfByParams(map[string]*schema.ParameterInfo{
			"challenge_id": StrParam("Challenge ID: license_check, flag_decrypt, algorithm_reversal, serial_gen, or math_puzzle", true),
			"answer":       StrParam("Your answer (the flag, key, serial number, or computed value)", true),
		}),
	}, nil
}

func (t *SubmitAnswerTool) InvokableRun(ctx context.Context, argumentsInJSON string, opts ...tool.Option) (string, error) {
	var input struct {
		ChallengeID string `json:"challenge_id"`
		Answer      string `json:"answer"`
	}
	if err := json.Unmarshal([]byte(argumentsInJSON), &input); err != nil {
		return "", fmt.Errorf("invalid input: %w", err)
	}

	validIDs := map[string]bool{
		"license_check":      true,
		"flag_decrypt":       true,
		"algorithm_reversal": true,
		"serial_gen":         true,
		"math_puzzle":        true,
	}
	if !validIDs[input.ChallengeID] {
		return fmt.Sprintf("Error: invalid challenge_id '%s'. Valid IDs: license_check, flag_decrypt, algorithm_reversal, serial_gen, math_puzzle", input.ChallengeID), nil
	}

	t.mu.Lock()
	t.Answers[input.ChallengeID] = input.Answer
	t.mu.Unlock()

	return fmt.Sprintf("Answer submitted for '%s': %s", input.ChallengeID, input.Answer), nil
}

// GetAnswers returns a copy of all submitted answers
func (t *SubmitAnswerTool) GetAnswers() map[string]string {
	t.mu.Lock()
	defer t.mu.Unlock()
	result := make(map[string]string, len(t.Answers))
	for k, v := range t.Answers {
		result[k] = v
	}
	return result
}
