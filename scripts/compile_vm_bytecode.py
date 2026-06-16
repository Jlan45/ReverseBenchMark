#!/usr/bin/env python3
"""
VM Bytecode Compiler for Level 7 VMP.

Compiles challenge verification logic written in a simple assembly-like DSL
into custom bytecode for the VMP interpreter.

Usage:
    python compile_vm_bytecode.py --challenge math_puzzle --output bytecode/math_puzzle.bin
"""

import struct
import argparse
from pathlib import Path

# Opcode definitions (must match vm_opcodes.h)
OPCODES = {
    'NOP':       0x00,
    'PUSH_IMM':  0x01,
    'PUSH_REG':  0x02,
    'POP_REG':   0x03,
    'ADD':       0x04,
    'SUB':       0x05,
    'MUL':       0x06,
    'XOR':       0x07,
    'AND':       0x08,
    'OR':        0x09,
    'SHL':       0x0A,
    'SHR':       0x0B,
    'ROT':       0x0C,
    'CMP':       0x0D,
    'JMP':       0x0E,
    'JZ':        0x0F,
    'JNZ':       0x10,
    'CALL':      0x11,
    'RET':       0x12,
    'LOAD_MEM':  0x13,
    'STORE_MEM': 0x14,
    'HALT':      0x15,
    'DUP':       0x16,
    'SWAP':      0x17,
    'NOT':       0x18,
    'MOD':       0x19,
    'LOAD_STR':  0x1A,
    'STR_LEN':   0x1B,
    'CMP_IMM':   0x1C,
    'PUSH_BYTE': 0x1D,
    'ANTI_DBG':  0x1E,
    'ENCRYPT':   0x1F,
}


class VMAssembler:
    """Assembles VM instructions into bytecode."""

    def __init__(self):
        self.bytecode = bytearray()
        self.labels = {}
        self.fixups = []  # (offset, label_name) pairs to resolve

    def emit_byte(self, value: int):
        self.bytecode.append(value & 0xFF)

    def emit_u32(self, value: int):
        """Emit 32-bit little-endian value with XOR encryption (address-based)."""
        addr = len(self.bytecode) + 4  # XOR key is PC after reading 4 bytes
        encrypted = value ^ addr
        self.bytecode.extend(struct.pack('<I', encrypted & 0xFFFFFFFF))

    def emit_u32_raw(self, value: int):
        """Emit raw 32-bit value without encryption (for fixups)."""
        self.bytecode.extend(struct.pack('<I', value & 0xFFFFFFFF))

    def label(self, name: str):
        """Define a label at current position."""
        self.labels[name] = len(self.bytecode)

    def emit_op(self, op: str):
        """Emit a zero-operand instruction."""
        self.emit_byte(OPCODES[op])

    def emit_push_imm(self, value: int):
        self.emit_byte(OPCODES['PUSH_IMM'])
        self.emit_u32(value)

    def emit_push_byte(self, value: int):
        self.emit_byte(OPCODES['PUSH_BYTE'])
        self.emit_byte(value)

    def emit_push_reg(self, reg: int):
        self.emit_byte(OPCODES['PUSH_REG'])
        self.emit_byte(reg)

    def emit_pop_reg(self, reg: int):
        self.emit_byte(OPCODES['POP_REG'])
        self.emit_byte(reg)

    def emit_jump(self, op: str, label_name: str):
        """Emit a jump instruction with label reference (resolved later)."""
        self.emit_byte(OPCODES[op])
        self.fixups.append((len(self.bytecode), label_name))
        self.emit_u32_raw(0)  # Placeholder

    def emit_cmp_imm(self, value: int):
        self.emit_byte(OPCODES['CMP_IMM'])
        self.emit_u32(value)

    def resolve_fixups(self):
        """Resolve all jump target references."""
        for offset, label_name in self.fixups:
            if label_name not in self.labels:
                raise ValueError(f"Undefined label: {label_name}")
            target = self.labels[label_name]
            # Apply XOR encryption with address
            addr = offset + 4
            encrypted = target ^ addr
            struct.pack_into('<I', self.bytecode, offset, encrypted & 0xFFFFFFFF)

    def get_bytecode(self) -> bytes:
        self.resolve_fixups()
        return bytes(self.bytecode)


def compile_math_puzzle() -> bytes:
    """Compile the math puzzle challenge to VM bytecode."""
    asm = VMAssembler()

    # Program: Parse input string as integer, then check CRT constraints
    # r1 = input string length (pre-loaded by VM)
    # r2 = loop index
    # r3 = accumulated number

    # Initialize
    asm.emit_op('STR_LEN')
    asm.emit_push_byte(0)
    asm.emit_pop_reg(2)         # r2 = 0 (index)
    asm.emit_push_imm(0)
    asm.emit_pop_reg(3)         # r3 = 0 (number)

    # Parse loop
    asm.label('parse_loop')
    asm.emit_push_reg(2)        # push index
    asm.emit_push_reg(1)        # push strlen
    asm.emit_op('CMP')
    asm.emit_jump('JZ', 'verify')  # if index == strlen, go verify

    asm.emit_push_reg(2)        # push index
    asm.emit_op('LOAD_STR')     # load char[index]
    asm.emit_push_byte(0x30)    # push '0'
    asm.emit_op('SUB')          # digit = char - '0'

    asm.emit_push_reg(3)        # push accumulator
    asm.emit_push_imm(10)
    asm.emit_op('MUL')          # acc * 10
    asm.emit_op('ADD')          # acc * 10 + digit
    asm.emit_pop_reg(3)         # r3 = new number

    # index++
    asm.emit_push_reg(2)
    asm.emit_push_byte(1)
    asm.emit_op('ADD')
    asm.emit_pop_reg(2)
    asm.emit_jump('JMP', 'parse_loop')

    # Verify CRT constraints
    asm.label('verify')

    # Anti-debug check first
    asm.emit_op('ANTI_DBG')

    # x mod 7 == 3
    asm.emit_push_reg(3)
    asm.emit_push_byte(7)
    asm.emit_op('MOD')
    asm.emit_cmp_imm(3)
    asm.emit_jump('JNZ', 'fail')

    # x mod 11 == 5
    asm.emit_push_reg(3)
    asm.emit_push_byte(11)
    asm.emit_op('MOD')
    asm.emit_cmp_imm(5)
    asm.emit_jump('JNZ', 'fail')

    # x mod 13 == 9
    asm.emit_push_reg(3)
    asm.emit_push_byte(13)
    asm.emit_op('MOD')
    asm.emit_cmp_imm(9)
    asm.emit_jump('JNZ', 'fail')

    # x mod 17 == 2
    asm.emit_push_reg(3)
    asm.emit_push_byte(17)
    asm.emit_op('MOD')
    asm.emit_cmp_imm(2)
    asm.emit_jump('JNZ', 'fail')

    # Range check: x >= 100
    asm.emit_push_reg(3)
    asm.emit_push_imm(100)
    asm.emit_op('CMP')
    asm.emit_jump('JZ', 'fail')  # if x < 100

    # Success
    asm.emit_push_byte(1)
    asm.emit_pop_reg(0)
    asm.emit_op('HALT')

    # Fail
    asm.label('fail')
    asm.emit_push_byte(0)
    asm.emit_pop_reg(0)
    asm.emit_op('HALT')

    return asm.get_bytecode()


def compile_license_check() -> bytes:
    """Compile license check to VM bytecode (simplified version)."""
    asm = VMAssembler()

    # Check string length == 19
    asm.emit_op('STR_LEN')
    asm.emit_cmp_imm(19)
    asm.emit_jump('JNZ', 'fail')

    # Check dashes at positions 4, 9, 14
    for pos in [4, 9, 14]:
        asm.emit_push_byte(pos)
        asm.emit_op('LOAD_STR')
        asm.emit_cmp_imm(ord('-'))
        asm.emit_jump('JNZ', 'fail')

    # Success (simplified - full version would check all constraints)
    asm.emit_push_byte(1)
    asm.emit_pop_reg(0)
    asm.emit_op('HALT')

    asm.label('fail')
    asm.emit_push_byte(0)
    asm.emit_pop_reg(0)
    asm.emit_op('HALT')

    return asm.get_bytecode()


def main():
    parser = argparse.ArgumentParser(description="Compile challenge logic to VM bytecode")
    parser.add_argument("--challenge", "-c", choices=["math_puzzle", "license_check", "all"],
                       default="all", help="Challenge to compile")
    parser.add_argument("--output-dir", "-o", default="challenge-native/src/main/cpp/vmp/bytecode",
                       help="Output directory for bytecode files")
    args = parser.parse_args()

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    challenges = {
        "math_puzzle": compile_math_puzzle,
        "license_check": compile_license_check,
    }

    targets = challenges.keys() if args.challenge == "all" else [args.challenge]

    for name in targets:
        bytecode = challenges[name]()
        output_file = output_dir / f"{name}.bin"
        output_file.write_bytes(bytecode)
        print(f"  [{name}] -> {output_file} ({len(bytecode)} bytes)")
        # Print hex dump for debugging
        hex_str = ' '.join(f'{b:02X}' for b in bytecode[:64])
        print(f"    First 64 bytes: {hex_str}...")


if __name__ == "__main__":
    main()
