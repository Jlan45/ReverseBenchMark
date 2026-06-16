package workspace

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
)

type Manifest struct {
	Level         int               `json:"level"`
	FileName      string            `json:"file_name"`
	DetectedType  string            `json:"detected_type"`
	Preprocessing []PreprocessStep  `json:"preprocessing,omitempty"`
	AgentHint     string            `json:"agent_hint,omitempty"`
	MCPBinaries   []string          `json:"mcp_binaries,omitempty"`
}

type PreprocessStep struct {
	Name       string `json:"name"`
	Status     string `json:"status"`
	OutputPath string `json:"output_path,omitempty"`
}

// Create sets up a workspace directory for a given level
func Create(baseDir string, level int) (string, error) {
	wsPath := filepath.Join(baseDir, fmt.Sprintf("level%d", level))
	dirs := []string{"input", "extracted", "normalized/native", "normalized/jadx_output", "artifacts", "state"}
	for _, d := range dirs {
		if err := os.MkdirAll(filepath.Join(wsPath, d), 0755); err != nil {
			return "", fmt.Errorf("create workspace dir %s: %w", d, err)
		}
	}
	return wsPath, nil
}

// WriteManifest persists the manifest to state/manifest.json
func WriteManifest(wsPath string, m *Manifest) error {
	data, err := json.MarshalIndent(m, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(filepath.Join(wsPath, "state", "manifest.json"), data, 0644)
}

// CopyAPK copies the APK file into workspace/input/
func CopyAPK(wsPath, apkPath string) (string, error) {
	data, err := os.ReadFile(apkPath)
	if err != nil {
		return "", fmt.Errorf("read APK: %w", err)
	}
	dst := filepath.Join(wsPath, "input", filepath.Base(apkPath))
	if err := os.WriteFile(dst, data, 0644); err != nil {
		return "", fmt.Errorf("write APK: %w", err)
	}
	return dst, nil
}
