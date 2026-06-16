package preprocess

import (
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
)

// JadxDecompile runs jadx to decompile DEX from an APK
func JadxDecompile(apkPath, outputDir string) error {
	os.MkdirAll(outputDir, 0755)

	cmd := exec.Command("jadx", "-d", outputDir, "--no-res", apkPath)
	output, err := cmd.CombinedOutput()
	if err != nil {
		if exitErr, ok := err.(*exec.ExitError); ok && exitErr.ExitCode() == 3 {
			// jadx exit 3 = completed with warnings
			log.Printf("[jadx] completed with warnings")
			return nil
		}
		return fmt.Errorf("jadx: %w\nOutput: %s", err, string(output))
	}
	return nil
}

// CheckJadx verifies jadx is available
func CheckJadx() bool {
	_, err := exec.LookPath("jadx")
	if err != nil {
		// Try common locations
		candidates := []string{
			filepath.Join(os.Getenv("HOME"), "jadx/bin/jadx"),
			"/usr/local/bin/jadx",
			"/opt/jadx/bin/jadx",
		}
		for _, c := range candidates {
			if _, err := os.Stat(c); err == nil {
				return true
			}
		}
		return false
	}
	return true
}
