package tools

import (
	"encoding/json"
	"fmt"
	"path/filepath"
	"strings"

	"github.com/cloudwego/eino/schema"
)

// ChrootPath provides virtual path translation for workspace sandboxing.
// The LLM sees paths rooted at "/workspace" which map to the real workspace directory.
type ChrootPath struct {
	RealRoot string
}

const VirtualRoot = "/workspace"

// ToReal converts a virtual path to a real filesystem path.
func (c *ChrootPath) ToReal(virtualPath string) (string, error) {
	absRoot, _ := filepath.Abs(c.RealRoot)
	p := virtualPath

	// If already a real absolute path under workspace, return it
	absP, _ := filepath.Abs(p)
	if strings.HasPrefix(absP, absRoot) {
		return absP, nil
	}

	// Strip /workspace prefix
	if p == VirtualRoot {
		return absRoot, nil
	}
	if strings.HasPrefix(p, VirtualRoot+"/") {
		p = p[len(VirtualRoot):]
	}

	rel := strings.TrimPrefix(filepath.Clean("/"+p), "/")
	real := absRoot
	if rel != "" {
		real = filepath.Join(absRoot, rel)
	}

	if !strings.HasPrefix(real, absRoot) {
		return "", fmt.Errorf("path '%s' is outside workspace", virtualPath)
	}
	return real, nil
}

// ToVirtual converts a real filesystem path to a virtual path.
func (c *ChrootPath) ToVirtual(realPath string) string {
	absRoot, _ := filepath.Abs(c.RealRoot)
	absReal, _ := filepath.Abs(realPath)
	rel, err := filepath.Rel(absRoot, absReal)
	if err != nil || strings.HasPrefix(rel, "..") {
		return realPath
	}
	if rel == "." {
		return VirtualRoot
	}
	return VirtualRoot + "/" + rel
}

// ScrubOutput replaces real workspace paths with virtual paths in output text.
func (c *ChrootPath) ScrubOutput(output string) string {
	absRoot, _ := filepath.Abs(c.RealRoot)
	result := strings.ReplaceAll(output, absRoot+"/", VirtualRoot+"/")
	result = strings.ReplaceAll(result, absRoot, VirtualRoot)
	return result
}

// ScrubCommand translates virtual paths in a shell command to real paths.
func (c *ChrootPath) ScrubCommand(cmd string) string {
	absRoot, _ := filepath.Abs(c.RealRoot)
	result := strings.ReplaceAll(cmd, VirtualRoot+"/", absRoot+"/")
	result = strings.ReplaceAll(result, VirtualRoot+" ", absRoot+" ")
	if strings.HasSuffix(result, VirtualRoot) {
		result = result[:len(result)-len(VirtualRoot)] + absRoot
	}
	return result
}

// TranslateArgsToReal translates virtual paths in JSON tool arguments to real paths.
func (c *ChrootPath) TranslateArgsToReal(argsJSON string) string {
	var args map[string]any
	if err := json.Unmarshal([]byte(argsJSON), &args); err != nil {
		return argsJSON
	}
	changed := false
	for k, v := range args {
		s, ok := v.(string)
		if !ok {
			continue
		}
		if strings.HasPrefix(s, VirtualRoot+"/") || s == VirtualRoot {
			real, err := c.ToReal(s)
			if err == nil {
				args[k] = real
				changed = true
			}
		}
	}
	if !changed {
		return argsJSON
	}
	out, _ := json.Marshal(args)
	return string(out)
}

// Helper functions for tool parameter definitions
func StrParam(desc string, required bool) *schema.ParameterInfo {
	return &schema.ParameterInfo{Type: schema.String, Desc: desc, Required: required}
}

func IntParam(desc string, required bool) *schema.ParameterInfo {
	return &schema.ParameterInfo{Type: schema.Integer, Desc: desc, Required: required}
}

func TruncateString(s string, maxLen int) string {
	if len(s) > maxLen {
		return s[:maxLen] + "...(truncated)"
	}
	return s
}
