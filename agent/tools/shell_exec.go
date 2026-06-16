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

type ShellExecTool struct {
	Chroot *ChrootPath
}

func (t *ShellExecTool) Info(ctx context.Context) (*schema.ToolInfo, error) {
	return &schema.ToolInfo{
		Name: "shell_exec",
		Desc: "Execute a shell command within the workspace. Working directory defaults to /workspace. Use for running analysis tools like file, strings, readelf, objdump, hexdump, etc.",
		ParamsOneOf: schema.NewParamsOneOfByParams(map[string]*schema.ParameterInfo{
			"command":     StrParam("Shell command to execute", true),
			"cwd":        StrParam("Working directory (default /workspace)", false),
			"timeout_sec": IntParam("Timeout in seconds (default 30, max 120)", false),
		}),
	}, nil
}

func (t *ShellExecTool) InvokableRun(ctx context.Context, argumentsInJSON string, opts ...tool.Option) (string, error) {
	var input struct {
		Command    string `json:"command"`
		Cwd        string `json:"cwd"`
		TimeoutSec int    `json:"timeout_sec"`
	}
	if err := json.Unmarshal([]byte(argumentsInJSON), &input); err != nil {
		return "", fmt.Errorf("invalid input: %w", err)
	}

	timeout := 30
	if input.TimeoutSec > 0 && input.TimeoutSec <= 120 {
		timeout = input.TimeoutSec
	}

	cwd := t.Chroot.RealRoot
	if input.Cwd != "" {
		realCwd, err := t.Chroot.ToReal(input.Cwd)
		if err != nil {
			return "", err
		}
		cwd = realCwd
	}

	realCmd := t.Chroot.ScrubCommand(input.Command)

	ctx, cancel := context.WithTimeout(ctx, time.Duration(timeout)*time.Second)
	defer cancel()

	cmd := exec.CommandContext(ctx, "sh", "-c", realCmd)
	cmd.Dir = cwd

	var stdout, stderr strings.Builder
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	start := time.Now()
	err := cmd.Run()
	duration := time.Since(start).Milliseconds()

	exitCode := 0
	if err != nil {
		if exitErr, ok := err.(*exec.ExitError); ok {
			exitCode = exitErr.ExitCode()
		} else {
			exitCode = -1
		}
	}

	result := map[string]any{
		"exit_code":   exitCode,
		"stdout":      t.Chroot.ScrubOutput(TruncateString(stdout.String(), 65536)),
		"stderr":      t.Chroot.ScrubOutput(TruncateString(stderr.String(), 65536)),
		"duration_ms": duration,
	}
	out, _ := json.Marshal(result)
	return string(out), nil
}
