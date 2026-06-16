#pragma once
#include "vm_opcodes.h"
#include <cstring>
#include <cstdint>
#include <cstdlib>

// Note: This file is #included from challenge_bridge.cpp (unity build).
// Functions like is_debugger_attached(), native_verify_license() etc.
// are already visible from earlier includes.

/**
 * Custom Stack-Based Virtual Machine Interpreter.
 *
 * Architecture:
 * - 256-entry operand stack (32-bit values)
 * - 64 general-purpose registers
 * - Variable-length instruction encoding
 * - Operand encryption (XOR with instruction address)
 *
 * This VM executes challenge verification logic compiled into custom bytecode,
 * making static analysis extremely difficult.
 */

struct VMState {
    uint32_t stack[VM_STACK_SIZE];
    int32_t sp;                    // Stack pointer
    uint32_t regs[VM_NUM_REGS];    // General registers
    uint8_t memory[VM_MEMORY_SIZE]; // VM memory
    uint32_t pc;                   // Program counter
    bool zero_flag;                // Comparison flag
    bool halted;                   // Execution state
    uint32_t call_stack[32];       // Call stack for subroutines
    int32_t csp;                   // Call stack pointer
};

class VMInterpreter {
public:
    VMInterpreter() {
        reset();
    }

    void reset() {
        memset(&state, 0, sizeof(state));
        state.sp = -1;
        state.csp = -1;
        state.halted = false;
    }

    /**
     * Execute bytecode with the given input string.
     * Returns the value in register 0 after execution (0 = success/verified).
     */
    int execute(const uint8_t* bytecode, size_t len, const char* input) {
        reset();

        // Make a mutable copy for self-modifying opcodes (OP_ENCRYPT)
        uint8_t* code = (uint8_t*)malloc(len);
        if (!code) return -1;
        memcpy(code, bytecode, len);

        // Store input string in VM memory
        size_t inputLen = strlen(input);
        if (inputLen >= VM_MEMORY_SIZE) inputLen = VM_MEMORY_SIZE - 1;
        memcpy(state.memory, input, inputLen);
        state.memory[inputLen] = 0;

        // Store input length in register 1
        state.regs[1] = (uint32_t)inputLen;

        while (!state.halted && state.pc < len) {
            uint8_t opcode = fetch_byte(code, len);

            switch (opcode) {
                case OP_NOP:
                    break;

                case OP_PUSH_IMM: {
                    uint32_t value = fetch_u32(code, len);
                    push(value);
                    break;
                }

                case OP_PUSH_BYTE: {
                    uint8_t value = fetch_byte(code, len);
                    push((uint32_t)value);
                    break;
                }

                case OP_PUSH_REG: {
                    uint8_t reg = fetch_byte(code, len);
                    push(state.regs[reg % VM_NUM_REGS]);
                    break;
                }

                case OP_POP_REG: {
                    uint8_t reg = fetch_byte(code, len);
                    state.regs[reg % VM_NUM_REGS] = pop();
                    break;
                }

                case OP_ADD: {
                    uint32_t b = pop(), a = pop();
                    push(a + b);
                    break;
                }

                case OP_SUB: {
                    uint32_t b = pop(), a = pop();
                    push(a - b);
                    break;
                }

                case OP_MUL: {
                    uint32_t b = pop(), a = pop();
                    push(a * b);
                    break;
                }

                case OP_XOR: {
                    uint32_t b = pop(), a = pop();
                    push(a ^ b);
                    break;
                }

                case OP_AND: {
                    uint32_t b = pop(), a = pop();
                    push(a & b);
                    break;
                }

                case OP_OR: {
                    uint32_t b = pop(), a = pop();
                    push(a | b);
                    break;
                }

                case OP_SHL: {
                    uint32_t b = pop(), a = pop();
                    push(a << (b & 31));
                    break;
                }

                case OP_SHR: {
                    uint32_t b = pop(), a = pop();
                    push(a >> (b & 31));
                    break;
                }

                case OP_ROT: {
                    uint32_t bits = pop(), val = pop();
                    bits &= 31;
                    push((val << bits) | (val >> (32 - bits)));
                    break;
                }

                case OP_CMP: {
                    uint32_t b = pop(), a = pop();
                    state.zero_flag = (a == b);
                    push(a); // Keep a on stack
                    break;
                }

                case OP_CMP_IMM: {
                    uint32_t imm = fetch_u32(code, len);
                    uint32_t a = pop();
                    state.zero_flag = (a == imm);
                    push(a);
                    break;
                }

                case OP_JMP: {
                    uint32_t target = fetch_u32(code, len);
                    state.pc = target;
                    break;
                }

                case OP_JZ: {
                    uint32_t target = fetch_u32(code, len);
                    if (state.zero_flag) state.pc = target;
                    break;
                }

                case OP_JNZ: {
                    uint32_t target = fetch_u32(code, len);
                    if (!state.zero_flag) state.pc = target;
                    break;
                }

                case OP_CALL: {
                    uint32_t target = fetch_u32(code, len);
                    if (state.csp < 31) {
                        state.call_stack[++state.csp] = state.pc;
                    }
                    state.pc = target;
                    break;
                }

                case OP_RET: {
                    if (state.csp >= 0) {
                        state.pc = state.call_stack[state.csp--];
                    } else {
                        state.halted = true;
                    }
                    break;
                }

                case OP_LOAD_MEM: {
                    uint32_t addr = pop();
                    if (addr < VM_MEMORY_SIZE) {
                        push((uint32_t)state.memory[addr]);
                    } else {
                        push(0);
                    }
                    break;
                }

                case OP_STORE_MEM: {
                    uint32_t val = pop();
                    uint32_t addr = pop();
                    if (addr < VM_MEMORY_SIZE) {
                        state.memory[addr] = (uint8_t)(val & 0xFF);
                    }
                    break;
                }

                case OP_LOAD_STR: {
                    // Load input char at index (from stack top)
                    uint32_t idx = pop();
                    if (idx < inputLen) {
                        push((uint32_t)(uint8_t)input[idx]);
                    } else {
                        push(0);
                    }
                    break;
                }

                case OP_STR_LEN: {
                    push((uint32_t)inputLen);
                    break;
                }

                case OP_HALT: {
                    state.halted = true;
                    break;
                }

                case OP_DUP: {
                    uint32_t val = pop();
                    push(val);
                    push(val);
                    break;
                }

                case OP_SWAP: {
                    uint32_t b = pop(), a = pop();
                    push(b);
                    push(a);
                    break;
                }

                case OP_NOT: {
                    push(~pop());
                    break;
                }

                case OP_MOD: {
                    uint32_t b = pop(), a = pop();
                    push(b != 0 ? a % b : 0);
                    break;
                }

                case OP_ANTI_DBG: {
#ifdef ANTI_DEBUG_ENABLED
                    if (is_debugger_attached() || is_ptrace_traced()) {
                        // Corrupt state if debugger detected
                        state.regs[0] = 0xDEADDEAD;
                        state.halted = true;
                    }
#endif
                    break;
                }

                case OP_ENCRYPT: {
                    // Self-modifying: XOR decrypt next N bytes of bytecode
                    // using the key from top of stack
                    uint8_t n = fetch_byte(code, len);
                    uint32_t key = pop();
                    uint8_t xor_key = (uint8_t)(key & 0xFF);
                    for (uint8_t i = 0; i < n && (state.pc + i) < len; i++) {
                        code[state.pc + i] ^= xor_key;
                    }
                    break;
                }

                default:
                    // Unknown opcode - halt
                    state.halted = true;
                    break;
            }
        }

        int result = (int)state.regs[0];
        free(code);
        return result;
    }

private:
    VMState state;
    size_t inputLen = 0;

    uint8_t fetch_byte(const uint8_t* code, size_t len) {
        if (state.pc < len) {
            return code[state.pc++];
        }
        state.halted = true;
        return 0;
    }

    uint32_t fetch_u32(const uint8_t* code, size_t len) {
        uint32_t value = 0;
        for (int i = 0; i < 4; i++) {
            value |= ((uint32_t)fetch_byte(code, len)) << (i * 8);
        }
        // XOR with instruction address for operand encryption
        value ^= (state.pc - 4);
        return value;
    }

    void push(uint32_t value) {
        if (state.sp < VM_STACK_SIZE - 1) {
            state.stack[++state.sp] = value;
        }
    }

    uint32_t pop() {
        if (state.sp >= 0) {
            return state.stack[state.sp--];
        }
        return 0;
    }
};

// ============================================================
// Pre-compiled VM bytecode for challenge verification
// ============================================================

/**
 * Bytecode for math puzzle verification:
 * Checks x mod 7 == 3, x mod 11 == 5, x mod 13 == 9, x mod 17 == 2
 *
 * Pseudocode:
 *   PUSH_REG r1 (input length)
 *   ... parse input as number ...
 *   PUSH value; PUSH 7; MOD; CMP_IMM 3; JNZ fail
 *   PUSH value; PUSH 11; MOD; CMP_IMM 5; JNZ fail
 *   ... etc
 *   PUSH 1; POP_REG r0; HALT  (success)
 *   fail: PUSH 0; POP_REG r0; HALT
 */
static const uint8_t VM_BYTECODE_MATH_PUZZLE[] = {
    // All 32-bit immediates are pre-encrypted: stored = desired ^ imm_start_addr
    // fetch_u32() decrypts via: value ^= (state.pc - 4) where pc-4 = imm_start_addr

    // Load string length, begin parsing
    OP_STR_LEN,                         // [0x00] push strlen
    OP_PUSH_BYTE, 0x00,                 // [0x01] push 0 (index)
    OP_POP_REG, 0x02,                   // [0x03] r2 = 0 (index)
    OP_PUSH_IMM, 0x06, 0x00, 0x00, 0x00, // [0x05] push 0 (enc: 0^0x06=0x06)
    OP_POP_REG, 0x03,                   // [0x0A] r3 = 0 (accumulator)

    // Parse loop: convert ASCII digits to number
    // Loop start addr = 0x0C
    OP_PUSH_REG, 0x02,                  // [0x0C] push index
    OP_PUSH_REG, 0x01,                  // [0x0E] push strlen
    OP_CMP,                             // [0x10] compare index == strlen
    OP_JZ, 0x21, 0x00, 0x00, 0x00,     // [0x11] if eq, jump to verify@0x33 (enc: 0x33^0x12=0x21)

    OP_PUSH_REG, 0x02,                  // [0x16] push index
    OP_LOAD_STR,                        // [0x18] load char at index
    OP_PUSH_BYTE, 0x30,                 // [0x19] push '0'
    OP_SUB,                             // [0x1B] char - '0' = digit

    OP_PUSH_REG, 0x03,                  // [0x1C] push accumulator
    OP_PUSH_IMM, 0x15, 0x00, 0x00, 0x00, // [0x1E] push 10 (enc: 0x0A^0x1F=0x15)
    OP_MUL,                             // [0x23] acc * 10
    OP_ADD,                             // [0x24] acc * 10 + digit
    OP_POP_REG, 0x03,                   // [0x25] r3 = new accumulator

    // Increment index
    OP_PUSH_REG, 0x02,                  // [0x27]
    OP_PUSH_BYTE, 0x01,                 // [0x29]
    OP_ADD,                             // [0x2B]
    OP_POP_REG, 0x02,                   // [0x2C]
    OP_JMP, 0x23, 0x00, 0x00, 0x00,    // [0x2E] jump to 0x0C (enc: 0x0C^0x2F=0x23)

    // Verify constraints (addr 0x33)
    // x mod 7 == 3
    OP_PUSH_REG, 0x03,                  // [0x33] push x
    OP_PUSH_BYTE, 0x07,                 // [0x35] push 7
    OP_MOD,                             // [0x37] x mod 7
    OP_CMP_IMM, 0x3A, 0x00, 0x00, 0x00, // [0x38] cmp with 3 (enc: 3^0x39=0x3A)
    OP_JNZ, 0x4A, 0x00, 0x00, 0x00,    // [0x3D] if != 3, fail@0x74 (enc: 0x74^0x3E=0x4A)

    // x mod 11 == 5
    OP_PUSH_REG, 0x03,                  // [0x42]
    OP_PUSH_BYTE, 0x0B,                 // [0x44] push 11
    OP_MOD,                             // [0x46]
    OP_CMP_IMM, 0x4D, 0x00, 0x00, 0x00, // [0x47] cmp with 5 (enc: 5^0x48=0x4D)
    OP_JNZ, 0x39, 0x00, 0x00, 0x00,    // [0x4C] fail@0x74 (enc: 0x74^0x4D=0x39)

    // x mod 13 == 9
    OP_PUSH_REG, 0x03,                  // [0x51]
    OP_PUSH_BYTE, 0x0D,                 // [0x53] push 13
    OP_MOD,                             // [0x55]
    OP_CMP_IMM, 0x5E, 0x00, 0x00, 0x00, // [0x56] cmp with 9 (enc: 9^0x57=0x5E)
    OP_JNZ, 0x28, 0x00, 0x00, 0x00,    // [0x5B] fail@0x74 (enc: 0x74^0x5C=0x28)

    // x mod 17 == 2
    OP_PUSH_REG, 0x03,                  // [0x60]
    OP_PUSH_BYTE, 0x11,                 // [0x62] push 17
    OP_MOD,                             // [0x64]
    OP_CMP_IMM, 0x64, 0x00, 0x00, 0x00, // [0x65] cmp with 2 (enc: 2^0x66=0x64)
    OP_JNZ, 0x1F, 0x00, 0x00, 0x00,    // [0x6A] fail@0x74 (enc: 0x74^0x6B=0x1F)

    // Success: r0 = 1
    OP_PUSH_BYTE, 0x01,                 // [0x6F]
    OP_POP_REG, 0x00,                   // [0x71]
    OP_HALT,                            // [0x73]

    // Fail (addr 0x74): r0 = 0
    OP_PUSH_BYTE, 0x00,                 // [0x74]
    OP_POP_REG, 0x00,                   // [0x76]
    OP_HALT,                            // [0x78]
};

static const size_t VM_BYTECODE_MATH_PUZZLE_LEN = sizeof(VM_BYTECODE_MATH_PUZZLE);

// ============================================================
// Dual-layer encrypted bytecode (Level 8)
// Layer A: OP_ENCRYPT self-modifying (verify section encrypted with key 0xC7)
// Layer B: Entire array XOR-encrypted with rotating multi-byte key
// ============================================================
#ifdef VMP_ENCRYPTED_BYTECODE

static const uint8_t VM_OUTER_KEY[] = {0xA5, 0x3C, 0x7E, 0x91, 0xF0, 0xD2, 0x4B, 0x68};

static const uint8_t VM_BYTECODE_MATH_PUZZLE_ENC[] = {
    0xBE, 0x21, 0x7E, 0x92, 0xF2, 0xD3, 0x4D, 0x68, 0xA5, 0x3C, 0x7D, 0x92, 0xF2, 0xD0, 0x49, 0x69,
    0xA8, 0x33, 0x5F, 0x91, 0xF0, 0xD2, 0x49, 0x6A, 0xBF, 0x21, 0x4E, 0x94, 0xF2, 0xD1, 0x4A, 0x7D,
    0xA5, 0x3C, 0x7E, 0x97, 0xF4, 0xD1, 0x48, 0x6A, 0xA7, 0x21, 0x7F, 0x95, 0xF3, 0xD0, 0x45, 0x4B,
    0xA5, 0x3C, 0x7E, 0x8C, 0x37, 0xCD, 0x0A, 0xAD, 0x61, 0xE6, 0xBE, 0x4F, 0x2B, 0x2B, 0x8C, 0xAF,
    0x62, 0xEB, 0x83, 0x56, 0x37, 0x15, 0x8E, 0xAC, 0x7F, 0xF0, 0xA0, 0x4A, 0x7E, 0x15, 0x8C, 0xAF,
    0x72, 0xD2, 0xB9, 0x56, 0x37, 0x17, 0x8F, 0xB2, 0x6F, 0xE2, 0xA5, 0x04, 0x37, 0x15, 0x8C, 0xBF,
    0x7A, 0xFB, 0xB9, 0x56, 0x35, 0x16, 0x91, 0xBE, 0x7B, 0xE7, 0xD1, 0x56, 0x37, 0x15, 0x9C, 0xB8,
    0x62, 0xFB, 0xB9, 0x4B, 0x36, 0x16, 0x8C, 0xBA, 0xB8, 0x3C, 0x7D, 0x91, 0xE5
};
static const size_t VM_BYTECODE_MATH_PUZZLE_ENC_LEN = sizeof(VM_BYTECODE_MATH_PUZZLE_ENC);

#endif // VMP_ENCRYPTED_BYTECODE

/**
 * Execute a VMP-protected challenge verification.
 * @param challengeIndex 0-4
 * @param input User-provided input to verify
 * @return true if verification passes
 */
static bool vm_execute_challenge(int challengeIndex, const char* input) {
    VMInterpreter vm;

    switch (challengeIndex) {
        case 4: {
#ifdef VMP_ENCRYPTED_BYTECODE
            // Dual-layer: decrypt outer layer, then VM self-decrypts inner via OP_ENCRYPT
            uint8_t* decrypted = (uint8_t*)malloc(VM_BYTECODE_MATH_PUZZLE_ENC_LEN);
            if (!decrypted) return false;
            for (size_t i = 0; i < VM_BYTECODE_MATH_PUZZLE_ENC_LEN; i++)
                decrypted[i] = VM_BYTECODE_MATH_PUZZLE_ENC[i] ^ VM_OUTER_KEY[i % sizeof(VM_OUTER_KEY)];
            int result = vm.execute(decrypted, VM_BYTECODE_MATH_PUZZLE_ENC_LEN, input);
            free(decrypted);
#else
            int result = vm.execute(VM_BYTECODE_MATH_PUZZLE, VM_BYTECODE_MATH_PUZZLE_LEN, input);
#endif
            return result == 1;
        }
        case 0:
            return native_verify_license(input);
        case 2:
            return native_verify_algorithm(input);
        default:
            return false;
    }
}
