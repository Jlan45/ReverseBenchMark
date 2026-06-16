package preprocess

import (
	"os"
	"path/filepath"
	"strings"
)

// ExtractNativeLibs finds and copies native SO libraries from the extracted APK
// Returns the list of copied SO paths (real filesystem paths)
func ExtractNativeLibs(extractedDir, nativeDir string) ([]string, error) {
	os.MkdirAll(nativeDir, 0755)

	libDir := filepath.Join(extractedDir, "lib")
	// Architecture priority order
	archPriority := []string{"arm64-v8a", "armeabi-v7a", "x86_64", "x86"}

	var selectedArch string
	for _, arch := range archPriority {
		archPath := filepath.Join(libDir, arch)
		if info, err := os.Stat(archPath); err == nil && info.IsDir() {
			selectedArch = arch
			break
		}
	}
	if selectedArch == "" {
		return nil, nil // no native libs
	}

	archPath := filepath.Join(libDir, selectedArch)
	entries, err := os.ReadDir(archPath)
	if err != nil {
		return nil, err
	}

	var soFiles []string
	for _, entry := range entries {
		if entry.IsDir() || !strings.HasSuffix(entry.Name(), ".so") {
			continue
		}
		src := filepath.Join(archPath, entry.Name())
		dst := filepath.Join(nativeDir, entry.Name())
		data, err := os.ReadFile(src)
		if err != nil {
			continue
		}
		if err := os.WriteFile(dst, data, 0644); err != nil {
			continue
		}
		soFiles = append(soFiles, dst)
	}
	return soFiles, nil
}
