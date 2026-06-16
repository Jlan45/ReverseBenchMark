package pipeline

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"time"

	benchagent "github.com/anthropic/reversebenchmark/agent/agent"
	"github.com/anthropic/reversebenchmark/agent/config"
	"github.com/anthropic/reversebenchmark/agent/preprocess"
	"github.com/anthropic/reversebenchmark/agent/workspace"
)

// LevelResult holds the output for a single level run
type LevelResult struct {
	Level    int               `json:"level"`
	Answers  map[string]string `json:"answers"`
	Steps    int               `json:"steps"`
	Duration time.Duration     `json:"duration"`
	Error    string            `json:"error,omitempty"`
}

// Pipeline orchestrates preprocessing and agent execution per level
type Pipeline struct {
	cfg *config.Config
}

func New(cfg *config.Config) *Pipeline {
	return &Pipeline{cfg: cfg}
}

// Run executes the full benchmark pipeline and returns results
func (p *Pipeline) Run() ([]LevelResult, error) {
	levels := []int{0, 1, 2, 3, 4, 5, 6, 7}
	if p.cfg.Level >= 0 {
		levels = []int{p.cfg.Level}
	}

	var results []LevelResult

	for _, level := range levels {
		r := p.runLevel(level)
		results = append(results, r)
	}

	// Write results JSON
	if err := p.writeResults(results); err != nil {
		log.Printf("[pipeline] warning: failed to write results: %v", err)
	}

	return results, nil
}

func (p *Pipeline) runLevel(level int) LevelResult {
	start := time.Now()
	result := LevelResult{Level: level, Answers: map[string]string{}}

	log.Printf("[pipeline] === Level %d ===", level)

	// Find APK
	apkPath := filepath.Join(p.cfg.APKDir, fmt.Sprintf("benchmark_level%d.apk", level))
	if _, err := os.Stat(apkPath); err != nil {
		result.Error = fmt.Sprintf("APK not found: %s", apkPath)
		result.Duration = time.Since(start)
		return result
	}

	// Create workspace
	baseDir := filepath.Join(os.TempDir(), "bench_ws")
	wsPath, err := workspace.Create(baseDir, level)
	if err != nil {
		result.Error = fmt.Sprintf("create workspace: %v", err)
		result.Duration = time.Since(start)
		return result
	}
	log.Printf("[pipeline] workspace: %s", wsPath)

	// Copy APK
	copiedAPK, err := workspace.CopyAPK(wsPath, apkPath)
	if err != nil {
		result.Error = fmt.Sprintf("copy APK: %v", err)
		result.Duration = time.Since(start)
		return result
	}

	// Preprocess
	if err := p.preprocess(wsPath, copiedAPK, level); err != nil {
		result.Error = fmt.Sprintf("preprocess: %v", err)
		result.Duration = time.Since(start)
		return result
	}

	// Write manifest before agent starts (so agent can read it)
	manifest := &workspace.Manifest{
		Level:        level,
		FileName:     filepath.Base(apkPath),
		DetectedType: "apk",
		AgentHint:    fmt.Sprintf("Level %d benchmark APK", level),
	}
	_ = workspace.WriteManifest(wsPath, manifest)

	// Start IDA MCP
	ctx := context.Background()
	var mcpInst *benchagent.MCPInstance
	if p.cfg.IDADir != "" {
		mcpInst, err = benchagent.StartIDAMCP(ctx, p.cfg.IDADir)
		if err != nil {
			log.Printf("[pipeline] warning: IDA MCP failed to start: %v (continuing without)", err)
		} else {
			defer mcpInst.Stop()
		}
	}

	// Run agent
	agent := benchagent.NewBenchmarkAgent(p.cfg, wsPath, level, mcpInst)
	answers, steps, err := agent.Run(ctx)
	result.Steps = steps
	result.Answers = answers
	if err != nil {
		result.Error = fmt.Sprintf("agent: %v", err)
	}

	result.Duration = time.Since(start)
	return result
}

func (p *Pipeline) preprocess(wsPath, apkPath string, level int) error {
	extractedDir := filepath.Join(wsPath, "extracted")

	// Unpack APK
	if err := preprocess.UnpackAPK(apkPath, extractedDir); err != nil {
		return fmt.Errorf("unpack: %w", err)
	}
	log.Printf("[pipeline] unpacked APK")

	// Run jadx for Java/Kotlin decompilation
	jadxOutput := filepath.Join(wsPath, "normalized", "jadx_output")
	if err := preprocess.JadxDecompile(apkPath, jadxOutput); err != nil {
		log.Printf("[pipeline] jadx warning (non-fatal): %v", err)
	} else {
		log.Printf("[pipeline] jadx decompiled")
	}

	// Extract native libraries (levels 4+)
	if level >= 4 {
		nativeDir := filepath.Join(wsPath, "normalized", "native")
		extracted, err := preprocess.ExtractNativeLibs(extractedDir, nativeDir)
		if err != nil {
			log.Printf("[pipeline] native extraction warning: %v", err)
		} else {
			log.Printf("[pipeline] extracted %d native libraries", len(extracted))
		}
	}

	return nil
}

func (p *Pipeline) writeResults(results []LevelResult) error {
	if err := os.MkdirAll(p.cfg.OutputDir, 0755); err != nil {
		return err
	}
	data, err := json.MarshalIndent(results, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(filepath.Join(p.cfg.OutputDir, "answers.json"), data, 0644)
}
