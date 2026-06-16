package com.benchmark.level7

import android.app.Activity
import android.os.Bundle
import android.os.Debug
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import com.benchmark.native_lib.NativeBridge

/**
 * Level 7: VMP (Virtual Machine Protection) + OLLVM + Anti-Debugging
 *
 * Maximum protection level:
 * - Challenge logic compiled to custom bytecode executed by a custom VM interpreter
 * - VM interpreter itself obfuscated by OLLVM (flattening + substitution + BCF)
 * - Anti-debugging measures active (ptrace, Frida detection, timing checks)
 * - Custom opcode set (not matching any known VM)
 * - Operand encryption (XOR with instruction address)
 * - R8 obfuscation on Java layer
 *
 * To reverse this, the AI must:
 * 1. Bypass/understand anti-debugging
 * 2. Deobfuscate the OLLVM-flattened native code
 * 3. Identify the VM interpreter dispatch loop
 * 4. Recover the custom opcode table
 * 5. Disassemble the VM bytecode
 * 6. Understand the challenge logic within the VM
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Multi-layer anti-debug
        if (Debug.isDebuggerConnected()) {
            corruptAndExit()
            return
        }
        if (NativeBridge.isDebuggerDetected()) {
            corruptAndExit()
            return
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val resultText = TextView(this).apply {
            text = "Level 7 - VMP Protected"
            textSize = 18f
        }
        layout.addView(resultText)

        val input = EditText(this).apply { hint = "Enter solution" }
        layout.addView(input)

        for (i in 0..4) {
            val btn = Button(this).apply {
                text = "VMP Challenge $i"
                setOnClickListener {
                    // Anti-debug re-check
                    if (NativeBridge.isDebuggerDetected()) {
                        resultText.text = "ACCESS DENIED"
                        return@setOnClickListener
                    }

                    // All verification goes through VMP
                    val ok = NativeBridge.vmpVerify(i, input.text.toString())
                    resultText.text = if (ok) "VM VERIFIED" else "VM REJECTED"
                }
            }
            layout.addView(btn)
        }

        setContentView(layout)
    }

    private fun corruptAndExit() {
        // Intentionally corrupt state and exit
        Thread.sleep(100)
        finish()
    }
}
