package agent

import "fmt"

// LevelDescription returns the protection description for a given level
func LevelDescription(level int) string {
	descs := []string{
		"No protection (plain Java code)",
		"R8 obfuscation (minified, renamed)",
		"R8 + ASM string encryption",
		"R8 + ASM control flow obfuscation",
		"Native JNI (C++ implementation)",
		"Native + OLLVM (control flow flattening, instruction substitution, bogus control flow)",
		"Native + OLLVM + Anti-debugging (ptrace, TracerPid, Frida detection, timing checks)",
		"Native + OLLVM + Anti-debugging + VMP (custom stack-based virtual machine)",
	}
	if level >= 0 && level < len(descs) {
		return descs[level]
	}
	return "Unknown"
}

// SystemPrompt builds the system prompt for the ReAct agent
func SystemPrompt(level int, hasIDA bool) string {
	prompt := `You are an expert Android reverse engineer. Your task is to analyze an APK and solve 5 reverse engineering challenges.

## Workspace
Your workspace root is /workspace. All paths start with /workspace.
  /workspace/input/           - The original APK
  /workspace/extracted/       - Unpacked APK (AndroidManifest.xml, classes.dex, lib/, etc.)
  /workspace/normalized/      - Analysis targets
    /workspace/normalized/jadx_output/sources/  - Decompiled Java source code
    /workspace/normalized/native/               - Extracted native SO libraries
  /workspace/artifacts/       - Pre-extracted information
  /workspace/state/           - Workspace metadata (manifest.json)

## Preprocessing Already Done (DO NOT repeat these steps):
- APK unpacked to /workspace/extracted/
- DEX decompiled via jadx to /workspace/normalized/jadx_output/sources/
- Native SO libraries extracted to /workspace/normalized/native/

## Challenges to Solve
You must solve ALL 5 challenges and submit answers using submit_answer():

1. **license_check** - Find a valid license key that passes verification
2. **flag_decrypt** - Find the encryption key and decrypt the hidden flag
3. **algorithm_reversal** - Reverse the custom algorithm (find input producing target output)
4. **serial_gen** - Generate correct serial number for username "benchmark_user"
5. **math_puzzle** - Find integer satisfying mathematical constraints (CRT)

## Strategy
1. Start by browsing the decompiled Java source to understand the app structure
2. Identify which challenges are in Java vs native code
3. For native challenges, use IDA tools to decompile the SO library
4. Use execute_python for computation (Z3 constraint solving, XOR decryption, CRT, etc.)
5. Submit each answer as you find it via submit_answer(challenge_id, answer)

## Important Rules
- DO NOT access files outside /workspace
- DO NOT re-run jadx or unzip (already done)
- When analyzing native code, first load the SO with idalib_open, then use decompile/disasm
- For math puzzles, prefer Z3 or direct computation via execute_python
- Submit answers as strings (not code)
- YOU MUST SOLVE ALL 5 CHALLENGES. Do NOT stop until you have called submit_answer for each one.
- NEVER output a text-only response until ALL 5 answers are submitted.
- After each answer submission, IMMEDIATELY move on to the next unsolved challenge.
- If you get stuck on one challenge, skip it temporarily and solve the others first, then come back.
`

	prompt += fmt.Sprintf("\n## Protection Level: %d\n%s\n", level, LevelDescription(level))

	if hasIDA {
		prompt += `
## IDA Tools Available
You have access to IDA Pro for binary analysis:
- idalib_open(path): Load a binary file for analysis. Use on SO files in /workspace/normalized/native/
- decompile(function_name_or_address): Get C pseudocode for a function
- disasm(address, count): Disassemble instructions at address
- lookup_funcs(pattern): Search function names matching pattern
- xrefs_to(address): Find cross-references TO an address
- callees(function): List functions called BY a function
- find_bytes(hex_pattern): Search for byte patterns in the binary
- basic_blocks(function): Get control flow graph basic blocks

Always load the SO first with idalib_open before using other IDA tools.
`
	}

	return prompt
}

// UserPrompt returns the initial user message for the agent
func UserPrompt(level int) string {
	return fmt.Sprintf(`Analyze this Level %d benchmark APK and solve all 5 challenges.

Start by examining the decompiled Java code at /workspace/normalized/jadx_output/sources/com/benchmark/.
Look for classes related to: LicenseChecker, FlagDecryptor, AlgorithmChallenge, SerialGenerator, MathPuzzle.

For levels 4+, the core logic is in native code — use idalib_open("/workspace/normalized/native/libchallenge_native.so") first.

You MUST call submit_answer(challenge_id, answer) for EACH of the 5 challenges. Do not stop working until all 5 are submitted.`, level)
}
