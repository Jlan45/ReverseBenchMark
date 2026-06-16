package tools

import (
	"context"
	"encoding/json"
	"fmt"
	"os"

	"github.com/cloudwego/eino/components/tool"
	"github.com/cloudwego/eino/schema"
)

type ReadFileTool struct {
	Chroot *ChrootPath
}

func (t *ReadFileTool) Info(ctx context.Context) (*schema.ToolInfo, error) {
	return &schema.ToolInfo{
		Name: "read_file",
		Desc: "Read file contents from the workspace. Paths start with /workspace. Large files are auto-truncated to 100KB.",
		ParamsOneOf: schema.NewParamsOneOfByParams(map[string]*schema.ParameterInfo{
			"path": StrParam("File path (e.g. /workspace/normalized/jadx_output/sources/com/...)", true),
		}),
	}, nil
}

func (t *ReadFileTool) InvokableRun(ctx context.Context, argumentsInJSON string, opts ...tool.Option) (string, error) {
	var input struct {
		Path string `json:"path"`
	}
	if err := json.Unmarshal([]byte(argumentsInJSON), &input); err != nil {
		return "", fmt.Errorf("invalid input: %w", err)
	}
	realPath, err := t.Chroot.ToReal(input.Path)
	if err != nil {
		return "", err
	}
	data, err := os.ReadFile(realPath)
	if err != nil {
		return "", fmt.Errorf("read file: %w", err)
	}
	const maxSize = 100 * 1024
	truncated := false
	if len(data) > maxSize {
		data = data[:maxSize]
		truncated = true
	}
	result := map[string]any{
		"content":   t.Chroot.ScrubOutput(string(data)),
		"size":      len(data),
		"truncated": truncated,
	}
	out, _ := json.Marshal(result)
	return string(out), nil
}
