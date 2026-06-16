package agent

import (
	"context"
	"fmt"
	"log"
	"strings"
	"time"

	"github.com/anthropic/reversebenchmark/agent/config"
	"github.com/anthropic/reversebenchmark/agent/tools"
	"github.com/cloudwego/eino/components/model"
	"github.com/cloudwego/eino/components/tool"
	"github.com/cloudwego/eino/compose"
	"github.com/cloudwego/eino/flow/agent/react"
	"github.com/cloudwego/eino/schema"
	openai "github.com/cloudwego/eino-ext/components/model/openai"
)

// BenchmarkAgent runs the ReAct agent for a single level
type BenchmarkAgent struct {
	cfg         *config.Config
	workspace   string
	level       int
	mcpInstance *MCPInstance
	submitTool  *tools.SubmitAnswerTool
	steps       int
}

// NewBenchmarkAgent creates an agent for analyzing a specific level
func NewBenchmarkAgent(cfg *config.Config, workspace string, level int, mcpInst *MCPInstance) *BenchmarkAgent {
	return &BenchmarkAgent{
		cfg:         cfg,
		workspace:   workspace,
		level:       level,
		mcpInstance: mcpInst,
		submitTool:  tools.NewSubmitAnswerTool(),
	}
}

// Run executes the agent and returns submitted answers
func (a *BenchmarkAgent) Run(ctx context.Context) (answers map[string]string, steps int, err error) {
	ctx, cancel := context.WithTimeout(ctx, a.cfg.AgentTimeout)
	defer cancel()

	chatModel, err := newChatModel(ctx, a.cfg)
	if err != nil {
		return nil, 0, fmt.Errorf("create model: %w", err)
	}

	chroot := &tools.ChrootPath{RealRoot: a.workspace}

	// Assemble tools
	agentTools := []tool.BaseTool{
		&tools.ShellExecTool{Chroot: chroot},
		&tools.ReadFileTool{Chroot: chroot},
		&tools.ListDirTool{Chroot: chroot},
		&tools.GrepWorkspaceTool{Chroot: chroot},
		&tools.ExecutePythonTool{Chroot: chroot},
		a.submitTool,
	}

	// Inject IDA MCP tools if available
	hasIDA := false
	if a.mcpInstance != nil && a.mcpInstance.Status == "ready" && len(a.mcpInstance.Tools) > 0 {
		agentTools = append(agentTools, a.mcpInstance.Tools...)
		hasIDA = true
		log.Printf("[agent] injected %d IDA MCP tools", len(a.mcpInstance.Tools))
	}

	// Build tool middleware for path translation and logging
	toolMW := compose.ToolMiddleware{
		Invokable: func(next compose.InvokableToolEndpoint) compose.InvokableToolEndpoint {
			return func(ctx context.Context, input *compose.ToolInput) (*compose.ToolOutput, error) {
				a.steps++

				// Translate virtual paths in MCP tool arguments
				translatedArgs := chroot.TranslateArgsToReal(input.Arguments)

				if a.cfg.Verbose {
					log.Printf("[tool] %s(%s)", input.Name, input.Arguments)
				}

				origArgs := input.Arguments
				input.Arguments = translatedArgs
				output, err := next(ctx, input)
				input.Arguments = origArgs

				if output != nil {
					output.Result = chroot.ScrubOutput(output.Result)
				}
				if err != nil {
					errMsg := fmt.Sprintf("Tool error: %v", chroot.ScrubOutput(err.Error()))
					if a.cfg.Verbose {
						log.Printf("[tool] %s → ERROR: %s", input.Name, errMsg)
					}
					return &compose.ToolOutput{Result: errMsg}, nil
				}
				if a.cfg.Verbose && output != nil {
					log.Printf("[tool] %s → %s", input.Name, tools.TruncateString(output.Result, 200))
				}
				return output, nil
			}
		},
	}

	// Create ReAct agent
	agent, err := react.NewAgent(ctx, &react.AgentConfig{
		ToolCallingModel: chatModel,
		ToolsConfig: compose.ToolsNodeConfig{
			Tools:               agentTools,
			ToolCallMiddlewares: []compose.ToolMiddleware{toolMW},
		},
		MaxStep: a.cfg.MaxSteps,
	})
	if err != nil {
		return nil, 0, fmt.Errorf("create agent: %w", err)
	}

	log.Printf("[agent] starting Level %d analysis (max %d steps, timeout %v)", a.level, a.cfg.MaxSteps, a.cfg.AgentTimeout)
	start := time.Now()

	// Initial messages
	messages := []*schema.Message{
		schema.SystemMessage(SystemPrompt(a.level, hasIDA)),
		schema.UserMessage(UserPrompt(a.level)),
	}

	// Retry loop: keep re-prompting until all 5 answers submitted or max retries
	const maxRetries = 5
	const requiredAnswers = 5

	for attempt := 0; attempt <= maxRetries; attempt++ {
		if attempt > 0 {
			// Build continuation prompt with status of submitted answers
			submitted := a.submitTool.GetAnswers()
			missing := missingChallenges(submitted)
			continueMsg := fmt.Sprintf(
				"You have submitted %d/5 answers so far. Missing challenges: %s. "+
					"Continue analyzing and submit the remaining answers using submit_answer(). "+
					"DO NOT output any explanation — just use tools to solve and submit.",
				len(submitted), missing)
			messages = append(messages, schema.UserMessage(continueMsg))
			log.Printf("[agent] retry %d: %d/%d answers, missing: %s", attempt, len(submitted), requiredAnswers, missing)
		}

		// Run agent (non-streaming)
		result, err := agent.Generate(ctx, messages)
		if err != nil {
			log.Printf("[agent] generate error: %v", err)
		}
		if result != nil && a.cfg.Verbose && result.Content != "" {
			log.Printf("[agent] response: %s", tools.TruncateString(result.Content, 500))
		}
		if result != nil && result.Content != "" {
			messages = append(messages, schema.AssistantMessage(result.Content, nil))
		}

		// Check if we have all answers
		if len(a.submitTool.GetAnswers()) >= requiredAnswers {
			break
		}

		// Check timeout
		if ctx.Err() != nil {
			break
		}
	}

	elapsed := time.Since(start)
	answers = a.submitTool.GetAnswers()
	log.Printf("[agent] Level %d complete: %d answers in %d steps (%v)", a.level, len(answers), a.steps, elapsed.Round(time.Second))

	return answers, a.steps, nil
}

func missingChallenges(submitted map[string]string) string {
	all := []string{"license_check", "flag_decrypt", "algorithm_reversal", "serial_gen", "math_puzzle"}
	var missing []string
	for _, id := range all {
		if _, ok := submitted[id]; !ok {
			missing = append(missing, id)
		}
	}
	return strings.Join(missing, ", ")
}

func newChatModel(ctx context.Context, cfg *config.Config) (model.ToolCallingChatModel, error) {
	return openai.NewChatModel(ctx, &openai.ChatModelConfig{
		Model:   cfg.LLMModel,
		APIKey:  cfg.LLMAPIKey,
		BaseURL: cfg.LLMBaseURL,
	})
}
