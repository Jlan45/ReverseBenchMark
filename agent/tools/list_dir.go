package tools

import (
	"context"
	"encoding/json"
	"os"
	"path/filepath"
	"strings"

	"github.com/cloudwego/eino/components/tool"
	"github.com/cloudwego/eino/schema"
)

type ListDirTool struct {
	Chroot *ChrootPath
}

func (t *ListDirTool) Info(ctx context.Context) (*schema.ToolInfo, error) {
	return &schema.ToolInfo{
		Name: "list_dir",
		Desc: "List workspace directory structure recursively. Default path is /workspace, default max_depth is 3.",
		ParamsOneOf: schema.NewParamsOneOfByParams(map[string]*schema.ParameterInfo{
			"path":      StrParam("Directory path (default /workspace)", false),
			"max_depth": IntParam("Max recursion depth (default 3)", false),
		}),
	}, nil
}

func (t *ListDirTool) InvokableRun(ctx context.Context, argumentsInJSON string, opts ...tool.Option) (string, error) {
	var input struct {
		Path     string `json:"path"`
		MaxDepth int    `json:"max_depth"`
	}
	json.Unmarshal([]byte(argumentsInJSON), &input)
	if input.Path == "" {
		input.Path = "/workspace"
	}
	if input.MaxDepth <= 0 {
		input.MaxDepth = 3
	}
	realDir, err := t.Chroot.ToReal(input.Path)
	if err != nil {
		return "", err
	}
	tree := buildTree(realDir, 0, input.MaxDepth)
	out, _ := json.Marshal(tree)
	return string(out), nil
}

type treeEntry struct {
	Name     string      `json:"name"`
	IsDir    bool        `json:"is_dir"`
	Size     int64       `json:"size,omitempty"`
	Children []treeEntry `json:"children,omitempty"`
}

func buildTree(dir string, depth, maxDepth int) []treeEntry {
	if depth >= maxDepth {
		return nil
	}
	entries, err := os.ReadDir(dir)
	if err != nil {
		return nil
	}
	var result []treeEntry
	for _, e := range entries {
		if strings.HasPrefix(e.Name(), ".") {
			continue
		}
		entry := treeEntry{Name: e.Name(), IsDir: e.IsDir()}
		if e.IsDir() {
			entry.Children = buildTree(filepath.Join(dir, e.Name()), depth+1, maxDepth)
		} else {
			info, _ := e.Info()
			if info != nil {
				entry.Size = info.Size()
			}
		}
		result = append(result, entry)
	}
	return result
}
