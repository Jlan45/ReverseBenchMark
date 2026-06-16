# OLLVM CMake Toolchain File for Android Cross-Compilation
# Uses OLLVM-patched clang with NDK sysroot for obfuscated Android builds.
#
# This file is included AFTER the NDK toolchain (via CMAKE_TOOLCHAIN_FILE pointing
# to the NDK), so we override only the compiler while keeping NDK sysroot/flags.

set(OLLVM_ROOT "${CMAKE_SOURCE_DIR}/../.ollvm/Hikari-LLVM15/build" CACHE PATH "Path to OLLVM build directory")

if(NOT EXISTS "${OLLVM_ROOT}/bin/clang")
    message(FATAL_ERROR "OLLVM clang not found at ${OLLVM_ROOT}/bin/clang. Run scripts/setup_ollvm.sh first.")
endif()

set(CMAKE_C_COMPILER "${OLLVM_ROOT}/bin/clang" CACHE FILEPATH "" FORCE)
set(CMAKE_CXX_COMPILER "${OLLVM_ROOT}/bin/clang++" CACHE FILEPATH "" FORCE)
