cmake_minimum_required(VERSION 3.22)
project(challenge_native CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optional OLLVM flags (passed from Gradle for Level 5+)
# These flags ONLY work with an OLLVM-patched clang (e.g., heroims/obfuscator).
# When using standard NDK clang, leave USE_OLLVM undefined.
if(DEFINED USE_OLLVM)
    message(STATUS "OLLVM obfuscation enabled (flags handled by compiler wrapper)")
    add_compile_options(
        -mllvm -fla
        -mllvm -sub
        -mllvm -bcf
        -mllvm -bcf_prob=40
    )
endif()

# Options for conditional compilation
option(ENABLE_ANTI_DEBUG "Enable anti-debugging checks" OFF)
option(ENABLE_VMP "Enable VMP interpreter" OFF)

# Unity build: challenge_bridge.cpp #includes all other source files.
# This ensures static functions are visible across the codebase.
set(CHALLENGE_SOURCES
    src/main/cpp/challenge_bridge.cpp
)

add_library(challenge_native SHARED ${CHALLENGE_SOURCES})

target_include_directories(challenge_native PRIVATE
    src/main/cpp
)

if(ENABLE_ANTI_DEBUG)
    target_compile_definitions(challenge_native PRIVATE ANTI_DEBUG_ENABLED=1)
endif()

if(ENABLE_VMP)
    target_compile_definitions(challenge_native PRIVATE VMP_ENABLED=1)
endif()

option(ENABLE_VMP_ENCRYPT "Enable VMP with dual-layer bytecode encryption" OFF)
if(ENABLE_VMP_ENCRYPT)
    target_compile_definitions(challenge_native PRIVATE VMP_ENABLED=1 VMP_ENCRYPTED_BYTECODE=1)
endif()

target_link_libraries(challenge_native
    android
    log
)
