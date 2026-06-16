package com.benchmark.level8

import android.app.Activity
import android.os.Bundle
import android.os.Debug
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import com.benchmark.native_lib.NativeBridge

/**
 * Level 8: VMP + Dual-Layer Bytecode Encryption + OLLVM + Anti-Debugging
 *
 * Beyond Level 7, this adds:
 * - Layer B: Entire VM bytecode stored XOR-encrypted in .rodata (invisible to static analysis)
 * - Layer A: Critical verification sections additionally encrypted via OP_ENCRYPT
 *   (self-modifying bytecode that decrypts at runtime)
 *
 * To reverse this, the AI must:
 * 1. Bypass/understand anti-debugging
 * 2. Deobfuscate the OLLVM-flattened native code
 * 3. Identify the VM interpreter dispatch loop
 * 4. Find and recover the outer encryption key + decrypt the bytecode blob
 * 5. Recover the custom opcode table
 * 6. Understand OP_ENCRYPT self-modification semantics
 * 7. Determine the inner encryption key and decrypt the verify section
 * 8. Analyze the fully-decrypted VM bytecode to extract challenge logic
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
            text = "Level 8 - VMP Encrypted"
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

                    // All verification goes through encrypted VMP
                    val ok = NativeBridge.vmpVerify(i, input.text.toString())
                    resultText.text = if (ok) "VM VERIFIED" else "VM REJECTED"
                }
            }
            layout.addView(btn)
        }

        setContentView(layout)
    }

    private fun corruptAndExit() {
        Thread.sleep(100)
        finish()
    }
}
