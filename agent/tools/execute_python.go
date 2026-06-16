package tools

import (
	"context"
	"encoding/json"
	"fmt"
	"os/exec"
	"strings"
	"time"

	"github.com/cloudwego/eino/components/tool"
	"github.com/cloudwego/eino/schema"
)

// ExecutePythonTool runs Python code for computation (Z3 solving, crypto, math)
type ExecutePythonTool struct {
	Chroot *ChrootPath
}

func (t *ExecutePythonTool) Info(ctx context.Context) (*schema.ToolInfo, error) {
	return &schema.ToolInfo{
		Name: "execute_python",
		Desc: `Execute Python 3 code for computation. Use this for:
- Z3 constraint solving (from z3 import *)
- Cryptographic operations (hashlib, Crypto)
- Mathematical computation (Chinese Remainder Theorem, modular arithmetic)
- Data transformation (XOR, base64, hex encoding)
The code should print its result to stdout.`,
		ParamsOneOf: schema.NewParamsOneOfByParams(map[string]*schema.ParameterInfo{
			"code": StrParam("Python 3 code to execute. Must print results to stdout.", true),
		}),
	}, nil
}

func (t *ExecutePythonTool) InvokableRun(ctx context.Context, argumentsInJSON string, opts ...tool.Option) (string, error) {
	var input struct {
		Code string `json:"code"`
	}
	if err := json.Unmarshal([]byte(argumentsInJSON), &input); err != nil {
		return "", fmt.Errorf("invalid input: %w", err)
	}

	ctx, cancel := context.WithTimeout(ctx, 30*time.Second)
	defer cancel()

	cmd := exec.CommandContext(ctx, "python3", "-c", input.Code)
	if t.Chroot != nil {
		cmd.Dir = t.Chroot.RealRoot
	}

	var stdout, stderr strings.Builder
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	err := cmd.Run()
	exitCode := 0
	if err != nil {
		if exitErr, ok := err.(*exec.ExitError); ok {
			exitCode = exitErr.ExitCode()
		} else {
			exitCode = -1
		}
	}

	result := map[string]any{
		"exit_code": exitCode,
		"stdout":    TruncateString(stdout.String(), 32768),
		"stderr":    TruncateString(stderr.String(), 8192),
	}
	out, _ := json.Marshal(result)
	return string(out), nil
}
