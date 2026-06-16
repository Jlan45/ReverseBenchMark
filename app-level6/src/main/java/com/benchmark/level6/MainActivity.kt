package com.benchmark.level6

import android.app.Activity
import android.os.Bundle
import android.os.Debug
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import com.benchmark.native_lib.NativeBridge

/**
 * Level 6: OLLVM + Anti-Debugging + Integrity Checks
 * Combines native obfuscation with runtime anti-debugging measures:
 * - ptrace self-trace detection
 * - /proc/self/status TracerPid check
 * - Frida gadget detection (/proc/self/maps)
 * - Timing-based single-step detection
 * - Java-level debugger check
 * - APK signature verification
 *
 * If any check fails, flag data is corrupted in memory.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Java-level anti-debug check
        if (Debug.isDebuggerConnected() || isDebugBuild()) {
            // Corrupt environment if debugger detected
            finish()
            return
        }

        // Native anti-debug check
        if (NativeBridge.isDebuggerDetected()) {
            finish()
            return
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val resultText = TextView(this).apply {
            text = "Level 6 - Anti-Debug Protected"
            textSize = 18f
        }
        layout.addView(resultText)

        val input = EditText(this).apply { hint = "Enter solution" }
        layout.addView(input)

        for (i in 0..4) {
            val btn = Button(this).apply {
                text = "Challenge $i"
                setOnClickListener {
                    // Re-check debugger before each verification
                    if (NativeBridge.isDebuggerDetected()) {
                        resultText.text = "Security violation detected"
                        return@setOnClickListener
                    }

                    val ok = when (i) {
                        0 -> NativeBridge.verifyLicense(input.text.toString())
                        2 -> NativeBridge.verifyAlgorithm(input.text.toString())
                        4 -> NativeBridge.verifyMathPuzzle(input.text.toString().toLongOrNull() ?: 0)
                        else -> false
                    }
                    resultText.text = if (ok) "VERIFIED" else "REJECTED"
                }
            }
            layout.addView(btn)
        }

        setContentView(layout)
    }

    private fun isDebugBuild(): Boolean {
        return try {
            val ai = packageManager.getApplicationInfo(packageName, 0)
            (ai.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }
}
