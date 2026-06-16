# ReverseBenchMark

AI 逆向工程能力的分级评测基准。通过 9 个渐进式保护等级的 Android APK，评估 AI Agent 在不同混淆和保护手段下的逆向分析能力。

## 保护等级

| Level | 保护方式 | 难度 |
|-------|---------|------|
| 0 | 无保护 — 纯 Kotlin，无混淆 | ⭐ |
| 1 | R8 代码缩减 + 基础混淆 | ⭐⭐ |
| 2 | 字符串加密 (ASM Transform) | ⭐⭐ |
| 3 | 控制流混淆 (ASM Transform) | ⭐⭐⭐ |
| 4 | Native JNI (C++17) | ⭐⭐⭐ |
| 5 | OLLVM (控制流平坦化 + 指令替换 + 虚假控制流) | ⭐⭐⭐⭐ |
| 6 | OLLVM + Anti-debug (ptrace/Frida/时序检测) | ⭐⭐⭐⭐ |
| 7 | OLLVM + Anti-debug + VMP (自定义虚拟机 + 操作数加密) | ⭐⭐⭐⭐⭐ |
| 8 | OLLVM + Anti-debug + VMP + 双层字节码加密 | ⭐⭐⭐⭐⭐ |

## 挑战内容

每个 Level 包含相同的 5 个逆向挑战，答案一致，仅保护手段递增：

| 挑战 | 类型 | 描述 |
|------|------|------|
| `license_check` | 密钥生成 | 满足校验和 + XOR + 加法约束的序列号 |
| `flag_decrypt` | 密文解密 | AES-128-CBC (Java) / XOR (Native) 解密 |
| `algorithm_reversal` | 哈希碰撞 | 自定义哈希函数求逆 |
| `serial_gen` | 序列号生成 | 根据用户名生成有效序列号 |
| `math_puzzle` | 数学求解 | 中国剩余定理 (CRT) 联立同余方程 |

## 前置条件

- **JDK 17+**
- **Android SDK** (compileSdk 34)
- **Gradle 8.6** (通过 wrapper 自动下载)
- **OLLVM NDK** (Level 5-8 需要，见下方说明)

## 快速开始

### 1. 克隆仓库

```bash
git clone https://github.com/Jlan45/ReverseBenchMark.git
cd ReverseBenchMark
```

### 2. 配置 Android SDK

确保设置了 `ANDROID_HOME` 环境变量，或在项目根目录创建 `local.properties`：

```properties
sdk.dir=/path/to/android/sdk
```

### 3. OLLVM 配置 (Level 5-8)

Level 5-8 需要 OLLVM 加持的 NDK。运行脚本会自动拉取 Hikari-LLVM15、构建 OLLVM clang/clang++，并生成本地 `.ollvm/ndk` overlay：

```bash
./scripts/setup_ollvm.sh
```

`.ollvm/` 是本地生成目录，已被 `.gitignore` 忽略，不应提交。

### 4. 构建所有 APK

```bash
./gradlew buildAllApks
```

构建产物输出到 `output/` 目录：

```
output/
├── benchmark_level0.apk
├── benchmark_level1.apk
├── ...
├── benchmark_level8.apk
└── ground_truth.json
```

### 5. 构建单个 Level

```bash
./gradlew :app-level0:assembleRelease    # 仅构建 Level 0
./gradlew :app-level8:assembleRelease    # 仅构建 Level 8
```

## Ground Truth

构建时自动生成 `output/ground_truth.json`，包含所有挑战的正确答案，用于评分：

```json
{
  "challenges": {
    "license_check": {"answer": "1000-006F-106F-106F"},
    "flag_decrypt": {"answer": "FLAG{a3s_d3crypt3d_s3cr3t_msg}"},
    "algorithm_reversal": {"answer": "FLAG{h4sh_c0ll1s10n_f0und}"},
    "serial_gen": {"answer": "SERIAL-51576-10642"},
    "math_puzzle": {"answer": "6275"}
  }
}
```

## 项目结构

```
ReverseBenchMark/
├── app-level0/ ~ app-level8/    # 9 个 APK 模块 (渐进式保护)
├── challenge-core/              # Kotlin 挑战逻辑 (Level 0-3)
├── challenge-native/            # Native C++ 挑战逻辑 (Level 4-8)
│   └── src/main/cpp/
│       ├── challenge_bridge.cpp # JNI 入口 (unity build)
│       ├── challenges/          # 各挑战实现
│       ├── anti_debug/          # 反调试检测
│       └── vmp/                 # 自定义虚拟机
│           ├── vm_opcodes.h     # 32 个自定义操作码
│           └── vm_interpreter.cpp
├── buildSrc/                    # Gradle 插件 (字符串加密/控制流混淆)
├── scripts/                     # 工具脚本
├── output/                      # 构建产物
└── agent/                       # AI Agent (评测工具)
```

## VMP 架构 (Level 7-8)

自定义栈式虚拟机，32 个操作码：

- **操作数加密**：所有 32 位立即数与指令地址 XOR
- **自修改字节码** (Level 8)：`OP_ENCRYPT` 指令在运行时解密后续字节码
- **双层加密** (Level 8)：
  - 外层：整个字节码数组用 8 字节 key 做 rotating XOR
  - 内层：关键验证段由 `OP_ENCRYPT` 在 VM 执行期间解密

## 评测方式

1. 将 APK 交给 AI Agent 分析
2. Agent 需要提取每个 challenge 的答案
3. 与 `ground_truth.json` 对比计算得分
4. 对比不同 Level 下的成功率，评估抗混淆能力

## License

MIT
