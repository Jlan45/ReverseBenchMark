package tools

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/cloudwego/eino/components/tool"
	"github.com/cloudwego/eino/schema"
)

type GrepWorkspaceTool struct {
	Chroot *ChrootPath
}

func (t *GrepWorkspaceTool) Info(ctx context.Context) (*schema.ToolInfo, error) {
	return &schema.ToolInfo{
		Name: "grep_workspace",
		Desc: "Search for a text pattern in workspace files. Returns matching lines with file paths and line numbers.",
		ParamsOneOf: schema.NewParamsOneOfByParams(map[string]*schema.ParameterInfo{
			"pattern": StrParam("Search pattern (substring match)", true),
			"path":    StrParam("Search subdirectory (e.g. /workspace/normalized), default /workspace", false),
		}),
	}, nil
}

func (t *GrepWorkspaceTool) InvokableRun(ctx context.Context, argumentsInJSON string, opts ...tool.Option) (string, error) {
	var input struct {
		Pattern string `json:"pattern"`
		Path    string `json:"path"`
	}
	if err := json.Unmarshal([]byte(argumentsInJSON), &input); err != nil {
		return "", fmt.Errorf("invalid input: %w", err)
	}
	searchPath := "/workspace"
	if input.Path != "" {
		searchPath = input.Path
	}
	realDir, err := t.Chroot.ToReal(searchPath)
	if err != nil {
		return "", err
	}

	var matches []string
	filepath.Walk(realDir, func(path string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() || info.Size() > 10*1024*1024 {
			return nil
		}
		data, err := os.ReadFile(path)
		if err != nil {
			return nil
		}
		lines := strings.Split(string(data), "\n")
		virtualPath := t.Chroot.ToVirtual(path)
		for i, line := range lines {
			if strings.Contains(line, input.Pattern) {
				matches = append(matches, fmt.Sprintf("%s:%d: %s", virtualPath, i+1, TruncateString(line, 200)))
				if len(matches) >= 100 {
					return fmt.Errorf("limit")
				}
			}
		}
		return nil
	})
	out, _ := json.Marshal(map[string]any{"matches": matches, "count": len(matches)})
	return string(out), nil
}
