package preprocess

import (
	"archive/zip"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
)

// UnpackAPK extracts an APK (zip) to the target directory
func UnpackAPK(apkPath, extractDir string) error {
	r, err := zip.OpenReader(apkPath)
	if err != nil {
		return fmt.Errorf("open APK: %w", err)
	}
	defer r.Close()

	for _, f := range r.File {
		target := filepath.Join(extractDir, f.Name)

		// Prevent zip slip
		if !strings.HasPrefix(filepath.Clean(target), filepath.Clean(extractDir)+string(os.PathSeparator)) {
			continue
		}

		if f.FileInfo().IsDir() {
			os.MkdirAll(target, 0755)
			continue
		}

		os.MkdirAll(filepath.Dir(target), 0755)

		outFile, err := os.OpenFile(target, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, f.Mode())
		if err != nil {
			return fmt.Errorf("create %s: %w", f.Name, err)
		}

		rc, err := f.Open()
		if err != nil {
			outFile.Close()
			return fmt.Errorf("open %s in zip: %w", f.Name, err)
		}

		_, err = io.Copy(outFile, rc)
		rc.Close()
		outFile.Close()
		if err != nil {
			return fmt.Errorf("extract %s: %w", f.Name, err)
		}
	}
	return nil
}
