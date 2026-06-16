#pragma once
#include <cstdint>

/**
 * Custom VM Opcode Definitions.
 * Stack-based virtual machine with 32 opcodes.
 */

// Opcode constants
#define OP_NOP       0x00
#define OP_PUSH_IMM  0x01  // Push 32-bit immediate
#define OP_PUSH_REG  0x02  // Push register value
#define OP_POP_REG   0x03  // Pop to register
#define OP_ADD       0x04  // Pop 2, push sum
#define OP_SUB       0x05  // Pop 2, push difference
#define OP_MUL       0x06  // Pop 2, push product
#define OP_XOR       0x07  // Pop 2, push XOR
#define OP_AND       0x08  // Pop 2, push AND
#define OP_OR        0x09  // Pop 2, push OR
#define OP_SHL       0x0A  // Shift left
#define OP_SHR       0x0B  // Shift right
#define OP_ROT       0x0C  // Rotate bits
#define OP_CMP       0x0D  // Compare, set flags
#define OP_JMP       0x0E  // Unconditional jump
#define OP_JZ        0x0F  // Jump if zero flag set
#define OP_JNZ       0x10  // Jump if zero flag not set
#define OP_CALL      0x11  // Call subroutine
#define OP_RET       0x12  // Return from subroutine
#define OP_LOAD_MEM  0x13  // Load from memory (input buffer)
#define OP_STORE_MEM 0x14  // Store to memory
#define OP_HALT      0x15  // Stop execution
#define OP_DUP       0x16  // Duplicate top of stack
#define OP_SWAP      0x17  // Swap top two stack items
#define OP_NOT       0x18  // Bitwise NOT
#define OP_MOD       0x19  // Modulo
#define OP_LOAD_STR  0x1A  // Load input string char at index
#define OP_STR_LEN   0x1B  // Push string length
#define OP_CMP_IMM   0x1C  // Compare top with immediate
#define OP_PUSH_BYTE 0x1D  // Push single byte
#define OP_ANTI_DBG  0x1E  // Trigger anti-debug check
#define OP_ENCRYPT   0x1F  // Self-modifying: decrypt next N bytes

// VM Constants
#define VM_STACK_SIZE  256
#define VM_NUM_REGS    64
#define VM_MAX_CODE    4096
#define VM_MEMORY_SIZE 1024
