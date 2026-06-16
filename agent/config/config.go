package config

import (
	"os"
	"strconv"
	"time"

	"github.com/joho/godotenv"
)

type Config struct {
	LLMType    string
	LLMModel   string
	LLMBaseURL string
	LLMAPIKey  string

	IDADir       string
	MaxSteps     int
	AgentTimeout time.Duration

	APKDir          string
	OutputDir       string
	GroundTruthFile string
	Level           int
	Verbose         bool
}

func Load() *Config {
	_ = godotenv.Load()

	cfg := &Config{
		LLMType:      getEnv("LLM_TYPE", "openai"),
		LLMModel:     getEnv("LLM_MODEL", "gpt-4o"),
		LLMBaseURL:   getEnv("LLM_BASE_URL", "https://api.openai.com/v1"),
		LLMAPIKey:    getEnv("LLM_API_KEY", ""),
		IDADir:       getEnv("IDADIR", ""),
		MaxSteps:     getEnvInt("MAX_STEPS", 100),
		AgentTimeout: time.Duration(getEnvInt("AGENT_TIMEOUT_MIN", 15)) * time.Minute,
		APKDir:       "../output",
		OutputDir:    "./results",
		Level:        -1,
		Verbose:      false,
	}
	return cfg
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func getEnvInt(key string, fallback int) int {
	if v := os.Getenv(key); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			return n
		}
	}
	return fallback
}
