# AGENTS.md

## 项目定位

ReverseBenchMark 是一个 Android 逆向工程能力分级评测基准。仓库通过 9 个渐进保护等级的 APK（`app-level0` 到 `app-level8`）承载同一组 5 个挑战，用于评估 AI Agent 在 Kotlin、R8/ProGuard、ASM 混淆、JNI、OLLVM、Anti-debug 和 VMP 保护下的分析能力。

本文件面向在本仓库中工作的编码代理。执行任务前请优先阅读本文件，再按需阅读 `README.md`、相关模块的 Gradle/CMake 配置和源码。

## 仓库结构

- `app-level0` 到 `app-level8`：9 个 APK 应用模块。每个模块是一个保护等级入口，包名为 `com.benchmark.levelN`。
- `challenge-core`：Kotlin 挑战实现，主要服务 Level 0-3。
- `challenge-native`：C++17/JNI 挑战实现，主要服务 Level 4-8。
- `buildSrc`：Gradle 插件源码，包含字符串加密和控制流混淆插件。
- `scripts`：辅助脚本，包括 OLLVM 设置、全量构建包装脚本、VMP 字节码编译脚本。
- `.ollvm`：本地生成的 OLLVM 源码、构建产物和 NDK overlay，由 `scripts/setup_ollvm.sh` 创建并被 Git 忽略。
- `evaluation`：旧版评测脚本和 `flags.json`。
- `agent`：Go 实现的自动化逆向评测 Agent。
- `output`：构建产物目录，通常由 `./gradlew buildAllApks` 生成，不应手写维护。

## 保护等级约定

- Level 0：无保护，纯 Kotlin，无混淆。
- Level 1：R8/ProGuard 代码缩减和基础混淆。
- Level 2：字符串加密，依赖 `buildSrc` 中的 ASM Transform 插件。
- Level 3：控制流混淆，依赖 `buildSrc` 中的 ASM Transform 插件。
- Level 4：Native JNI，挑战逻辑迁移到 C++。
- Level 5：Native + OLLVM。
- Level 6：OLLVM + Anti-debug。
- Level 7：OLLVM + Anti-debug + VMP。
- Level 8：OLLVM + Anti-debug + VMP + 双层字节码加密。

新增、删除或调整保护能力时，要同步检查对应 `app-levelN/build.gradle.kts`、`challenge-native/CMakeLists.txt`、C++ 宏开关、README 和评测说明。

## 常用命令

项目使用 Gradle wrapper。优先使用 wrapper，不要假设系统 Gradle 版本一致。

```bash
./gradlew buildAllApks
```

构建所有 Level 的 release APK，并收集到 `output/`：

- `output/benchmark_level0.apk`
- ...
- `output/benchmark_level8.apk`
- `output/ground_truth.json`

构建单个 Level：

```bash
./gradlew :app-level0:assembleRelease
./gradlew :app-level8:assembleRelease
```

运行常规 Gradle 校验：

```bash
./gradlew build
```

重新编译 VMP 字节码：

```bash
python3 scripts/compile_vm_bytecode.py --challenge all
```

运行旧版评测脚本：

```bash
python3 evaluation/evaluate.py --submission submission.json --truth evaluation/flags.json
```

运行 Go Agent（在 `agent/` 下）：

```bash
cd agent
LLM_API_KEY=... IDADIR=/path/to/ida go run . \
  --apk-dir ../output \
  --ground-truth ../output/ground_truth.json
```

## 环境要求

- JDK 17+。
- Android SDK，compileSdk/targetSdk 为 34。
- Android build tools 34.0.0。
- CMake 3.22.1。
- Level 4-8 需要 NDK，ABI 为 `arm64-v8a` 和 `armeabi-v7a`。
- Level 5-8 期望先运行 `./scripts/setup_ollvm.sh`，生成 `.ollvm/ndk`。不要提交 `.ollvm/` 内容。
- Go Agent 当前 `go.mod` 声明 Go `1.25.5`。运行 Agent 前检查本机 Go 版本是否满足。

Android SDK 可通过环境变量 `ANDROID_HOME` 或根目录 `local.properties` 中的 `sdk.dir=/path/to/android/sdk` 指定。

## Ground Truth 和答案一致性

5 个 challenge 在所有 Level 中语义应保持一致。修改答案、flag、校验算法或输入格式时，必须同时检查并同步：

- `challenge-core/src/main/java/com/benchmark/core/challenges/*`
- `challenge-native/src/main/cpp/challenges/*`
- `build.gradle.kts` 中的 `generateGroundTruth(...)`
- `evaluation/flags.json`
- `README.md` 的挑战说明和示例答案
- `agent/main.go` 的 ground truth JSON 结构假设

注意当前仓库存在两套 ground truth 口径：

- Gradle `buildAllApks` 生成 `output/ground_truth.json`，Agent 默认读取它。
- `evaluation/flags.json` 是旧版评测文件，当前只描述 Level 0-7，且部分示例值可能与 Gradle 生成结果不同。

除非任务明确要求维护旧版评测，否则以 `./gradlew buildAllApks` 生成的 `output/ground_truth.json` 作为权威运行口径。若修改挑战答案，应主动消除两套口径的不一致。

## 构建和验证准则

修改代码后按影响范围选择最小但充分的验证：

- 修改 Kotlin challenge 或 app 层：至少运行受影响模块的 `assembleRelease`。
- 修改 `challenge-native`、CMake、Anti-debug、VMP 或 OLLVM 开关：至少运行一个受影响 native Level 的 `assembleRelease`，高风险改动运行 `./gradlew buildAllApks`。
- 修改 `buildSrc` 插件：运行至少一个使用插件的 Level（Level 2 或 Level 3）构建。
- 修改全局 Gradle、签名、SDK/NDK 配置或 ground truth：运行 `./gradlew buildAllApks`。
- 修改 Go Agent：在 `agent/` 下运行 `go test ./...`；如果没有测试，至少运行 `go test ./...` 作为编译校验。
- 修改 Python 脚本：用 `python3 script.py --help` 或实际最小输入验证。

CI 期望 `./gradlew buildAllApks --no-daemon` 能生成 9 个 APK。不要把只生成 8 个 APK 的流程当作完整验证。

## 脚本注意事项

- `scripts/build_all.sh` 是包装脚本，但当前收集循环只覆盖 Level 0-7；根 Gradle 任务 `buildAllApks` 覆盖 Level 0-8。完整构建请优先使用 `./gradlew buildAllApks`。
- `scripts/compile_vm_bytecode.py` 的 opcode 定义必须与 `challenge-native/src/main/cpp/vmp/vm_opcodes.h` 保持一致。
- `challenge-native/CMakeLists.txt` 使用 unity build：只把 `challenge_bridge.cpp` 作为源文件加入 target，其他 C++ 文件通过 include 方式纳入。新增 C++ 文件前先理解现有 include 模式。

## 编码规范

- Kotlin 使用官方 Kotlin style，JVM target 为 17。
- C++ 使用 C++17，保持 Android NDK 兼容。
- Gradle 配置使用 Kotlin DSL。
- 优先沿用现有包名、命名、模块边界和构建方式。
- 不要随意更改 applicationId、namespace、minSdk、targetSdk 或 ABI 列表，除非任务明确要求。
- 不要把构建产物、反编译产物、临时 workspace 或大体积二进制提交到源码目录。
- 保持挑战语义跨 Kotlin 和 Native 一致；保护强度可以递增，但答案和挑战目标不应意外漂移。
- 修改混淆或保护逻辑时，避免让低 Level 获得更高 Level 的保护，也避免让高 Level 失去预期保护。

## Android 模块约束

- App 模块的 release 签名由根 `build.gradle.kts` 统一配置，使用用户目录下 Android debug keystore。
- Level 0 依赖 `challenge-core`，无 minify。
- Level 1-3 通常依赖 `challenge-core`，并通过 R8/ProGuard 与 `buildSrc` 插件增加保护。
- Level 4-8 通常依赖 `challenge-native`，通过 Gradle `externalNativeBuild` 指向 `challenge-native/CMakeLists.txt`。
- Level 5-8 使用 `ndkPath = "${rootDir}/.ollvm/ndk"`，该目录由 `scripts/setup_ollvm.sh` 生成。
- Native Level 的 CMake 参数和宏定义控制 Anti-debug、VMP、VMP 加密、OLLVM 等行为。调整宏时同步检查 C++ 条件编译路径。

## Agent 子项目

`agent/` 是独立 Go module，用于自动分析 APK 并提交答案。

关键入口：

- `agent/main.go`：CLI、ground truth 加载、结果评分。
- `agent/config/config.go`：环境变量和默认配置。
- `agent/pipeline`：多 Level 运行流程。
- `agent/preprocess`：APK 解包、JADX、native 预处理。
- `agent/tools`：Agent 可用工具，包括文件读取、grep、shell、Python 执行和答案提交。

运行 Agent 需要：

- `LLM_API_KEY`
- `IDADIR`
- 可选 `LLM_TYPE`、`LLM_MODEL`、`LLM_BASE_URL`、`MAX_STEPS`、`AGENT_TIMEOUT_MIN`

默认 `--apk-dir` 为 `../output`，默认 ground truth 为 APK 目录下的 `ground_truth.json`。

## Git 和文件操作

- 工作区可能已有用户改动。不要还原、覆盖或格式化与当前任务无关的文件。
- 编辑前查看相关文件当前内容，避免覆盖用户正在进行的修改。
- 优先使用 `rg` / `rg --files` 搜索。
- 根目录 `output/`、各模块 `build/`、Agent 运行结果和临时反编译目录通常视为生成物。
- 若必须执行会联网下载依赖、安装 SDK/NDK、删除文件或修改仓库外路径的操作，应先征得用户许可。

## 提交前检查清单

完成改动前至少确认：

- 影响范围内的模块能构建或有明确说明为何未验证。
- APK 数量要求未破坏：完整构建应产出 Level 0-8 共 9 个 APK。
- `ground_truth.json` 生成逻辑与 challenge 实现一致。
- README、`evaluation/flags.json` 和 Agent 假设没有被改动制造新的不一致。
- OLLVM/Anti-debug/VMP 相关改动没有意外影响低等级 APK。
- 没有提交本地 SDK 路径、API key、IDA 路径或其他机器特定配置。
