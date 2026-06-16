#!/bin/bash
# =============================================================================
# Build All Benchmark APKs
# Builds APKs for all 8 protection levels and collects them in output/
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_DIR="${ROOT_DIR}/output/${TIMESTAMP}"

echo "=============================================="
echo "  ReverseBenchMark - Build All APKs"
echo "=============================================="
echo ""
echo "Output directory: ${OUTPUT_DIR}"
echo ""

mkdir -p "${OUTPUT_DIR}"

cd "${ROOT_DIR}"

# Build all release APKs
echo "[*] Building all modules..."
./gradlew buildAllApks --no-daemon

# Collect APKs
echo ""
echo "[*] Collecting APKs..."
for level in 0 1 2 3 4 5 6 7; do
    APK_PATH="app-level${level}/build/outputs/apk/release/"
    APK_FILE=$(find "${APK_PATH}" -name "*.apk" 2>/dev/null | head -1)

    if [ -n "${APK_FILE}" ] && [ -f "${APK_FILE}" ]; then
        DEST="${OUTPUT_DIR}/benchmark_level${level}.apk"
        cp "${APK_FILE}" "${DEST}"
        SIZE=$(du -h "${DEST}" | cut -f1)
        echo "  [✓] Level ${level}: benchmark_level${level}.apk (${SIZE})"
    else
        echo "  [✗] Level ${level}: APK not found (build may have failed)"
    fi
done

# Generate checksums
echo ""
echo "[*] Generating checksums..."
cd "${OUTPUT_DIR}"
shasum -a 256 *.apk > checksums.sha256 2>/dev/null || true
cd "${ROOT_DIR}"

# Copy ground truth for reference
cp evaluation/flags.json "${OUTPUT_DIR}/flags.json"

echo ""
echo "=============================================="
echo "  Build Complete!"
echo "  APKs: ${OUTPUT_DIR}/"
echo "  Checksums: ${OUTPUT_DIR}/checksums.sha256"
echo "=============================================="
