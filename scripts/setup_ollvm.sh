#!/usr/bin/env bash
# =============================================================================
# Setup OLLVM (Hikari-LLVM15) for Android native benchmark builds.
#
# This script creates a local, ignored .ollvm/ directory:
#   .ollvm/Hikari-LLVM15/  cloned upstream source
#   .ollvm/Hikari-LLVM15/build/  built OLLVM clang/clang++
#   .ollvm/ndk/  generated Android NDK overlay whose clang/clang++ invoke OLLVM
#
# It intentionally avoids committing toolchain files or symlinks into the repo.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
WORK_DIR="${OLLVM_WORK_DIR:-${ROOT_DIR}/.ollvm}"
REPO_URL="${OLLVM_REPO_URL:-https://github.com/nicedayzhu/Hikari-LLVM15.git}"
NDK_VERSION="${ANDROID_NDK_VERSION:-26.1.10909125}"
HIKARI_DIR="${WORK_DIR}/Hikari-LLVM15"
BUILD_DIR="${HIKARI_DIR}/build"
OVERLAY_NDK_DIR="${WORK_DIR}/ndk"

usage() {
    cat <<EOF
Usage: $0 [--skip-build]

Environment:
  OLLVM_WORK_DIR       Local output directory. Default: ${ROOT_DIR}/.ollvm
  OLLVM_REPO_URL       OLLVM repository URL. Default: ${REPO_URL}
  ANDROID_NDK_HOME     Existing Android NDK path to wrap.
  ANDROID_HOME         Android SDK root used to find ndk/${NDK_VERSION}.
  ANDROID_SDK_ROOT     Android SDK root fallback.
  ANDROID_NDK_VERSION  NDK version under the SDK. Default: ${NDK_VERSION}

Options:
  --skip-build         Clone/update and generate the overlay, but do not build LLVM.
EOF
}

SKIP_BUILD=0
for arg in "$@"; do
    case "$arg" in
        --skip-build) SKIP_BUILD=1 ;;
        -h|--help) usage; exit 0 ;;
        *) echo "[ERROR] Unknown argument: $arg" >&2; usage; exit 1 ;;
    esac
done

find_sdk_dir() {
    if [ -f "${ROOT_DIR}/local.properties" ]; then
        local sdk
        sdk="$(sed -n 's/^sdk\.dir=//p' "${ROOT_DIR}/local.properties" | tail -1)"
        if [ -n "$sdk" ]; then
            printf '%s\n' "$sdk"
            return 0
        fi
    fi
    if [ -n "${ANDROID_HOME:-}" ]; then
        printf '%s\n' "$ANDROID_HOME"
        return 0
    fi
    if [ -n "${ANDROID_SDK_ROOT:-}" ]; then
        printf '%s\n' "$ANDROID_SDK_ROOT"
        return 0
    fi
    return 1
}

find_host_tag() {
    case "$(uname -s)" in
        Darwin) printf 'darwin-x86_64\n' ;;
        Linux) printf 'linux-x86_64\n' ;;
        *) echo "[ERROR] Unsupported host OS: $(uname -s)" >&2; exit 1 ;;
    esac
}

require_tool() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "[ERROR] $1 is required but was not found." >&2
        exit 1
    fi
}

create_compiler_wrapper() {
    local path="$1"
    local compiler="$2"
    cat > "$path" <<EOF
#!/usr/bin/env bash
set -euo pipefail
tool_name="\$(basename "\$0")"
exec -a "\$tool_name" "${BUILD_DIR}/bin/${compiler}" "\$@"
EOF
    chmod +x "$path"
}

link_dir_contents() {
    local src="$1"
    local dst="$2"
    local skip_name="${3:-}"
    mkdir -p "$dst"
    for entry in "$src"/*; do
        local name
        name="$(basename "$entry")"
        if [ "$name" = "$skip_name" ]; then
            continue
        fi
        ln -s "$entry" "${dst}/${name}"
    done
}

create_ndk_overlay() {
    local ndk_src="$1"
    local host_tag="$2"
    local src_prebuilt="${ndk_src}/toolchains/llvm/prebuilt/${host_tag}"
    local dst_prebuilt="${OVERLAY_NDK_DIR}/toolchains/llvm/prebuilt/${host_tag}"

    if [ ! -f "${ndk_src}/source.properties" ]; then
        echo "[ERROR] Invalid NDK path: ${ndk_src}" >&2
        exit 1
    fi
    if [ ! -d "${src_prebuilt}/bin" ]; then
        echo "[ERROR] NDK prebuilt host directory not found: ${src_prebuilt}" >&2
        exit 1
    fi

    rm -rf "$OVERLAY_NDK_DIR"
    mkdir -p "$OVERLAY_NDK_DIR"

    link_dir_contents "$ndk_src" "$OVERLAY_NDK_DIR" "toolchains"
    mkdir -p "${OVERLAY_NDK_DIR}/toolchains/llvm/prebuilt"

    link_dir_contents "$src_prebuilt" "$dst_prebuilt" "bin"
    link_dir_contents "${src_prebuilt}/bin" "${dst_prebuilt}/bin"

    rm -f "${dst_prebuilt}/bin/clang" "${dst_prebuilt}/bin/clang++"
    create_compiler_wrapper "${dst_prebuilt}/bin/clang" "clang"
    create_compiler_wrapper "${dst_prebuilt}/bin/clang++" "clang++"
}

echo "=============================================="
echo "  Setup OLLVM Toolchain (Hikari-LLVM15)"
echo "=============================================="

require_tool git
require_tool cmake

if command -v ninja >/dev/null 2>&1; then
    GENERATOR="Ninja"
    BUILD_CMD=(ninja)
else
    GENERATOR="Unix Makefiles"
    BUILD_CMD=(make)
fi

SDK_DIR="$(find_sdk_dir || true)"
NDK_SRC="${ANDROID_NDK_HOME:-}"
if [ -z "$NDK_SRC" ]; then
    if [ -z "$SDK_DIR" ]; then
        echo "[ERROR] ANDROID_HOME, ANDROID_SDK_ROOT, local.properties sdk.dir, or ANDROID_NDK_HOME is required." >&2
        exit 1
    fi
    NDK_SRC="${SDK_DIR}/ndk/${NDK_VERSION}"
fi

HOST_TAG="$(find_host_tag)"
mkdir -p "$WORK_DIR"

if [ ! -d "${HIKARI_DIR}/.git" ]; then
    echo "[*] Cloning Hikari-LLVM15..."
    git clone --depth 1 "$REPO_URL" "$HIKARI_DIR"
else
    echo "[*] Updating Hikari-LLVM15..."
    git -C "$HIKARI_DIR" pull --ff-only
fi

if [ "$SKIP_BUILD" -eq 0 ]; then
    echo "[*] Building OLLVM clang/clang++..."
    mkdir -p "$BUILD_DIR"
    cmake -S "${HIKARI_DIR}/llvm" -B "$BUILD_DIR" -G "$GENERATOR" \
        -DCMAKE_BUILD_TYPE=Release \
        -DLLVM_ENABLE_PROJECTS=clang \
        -DLLVM_TARGETS_TO_BUILD="AArch64;ARM;X86" \
        -DLLVM_INCLUDE_TESTS=OFF \
        -DLLVM_INCLUDE_EXAMPLES=OFF \
        -DLLVM_INCLUDE_BENCHMARKS=OFF

    CPU_COUNT="$(getconf _NPROCESSORS_ONLN 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)"
    "${BUILD_CMD[@]}" -C "$BUILD_DIR" -j"$CPU_COUNT"
elif [ ! -x "${BUILD_DIR}/bin/clang" ] || [ ! -x "${BUILD_DIR}/bin/clang++" ]; then
    echo "[ERROR] --skip-build requested, but OLLVM clang/clang++ do not exist in ${BUILD_DIR}/bin." >&2
    exit 1
fi

echo "[*] Generating Android NDK overlay..."
create_ndk_overlay "$NDK_SRC" "$HOST_TAG"

echo ""
echo "=============================================="
echo "  OLLVM setup complete"
echo "  LLVM build: ${BUILD_DIR}"
echo "  NDK overlay: ${OVERLAY_NDK_DIR}"
echo ""
echo "  Build protected APKs with:"
echo "    ./gradlew buildAllApks"
echo "=============================================="
