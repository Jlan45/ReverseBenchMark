package com.benchmark.level5

import android.app.Activity
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import com.benchmark.native_lib.NativeBridge

/**
 * Level 5: OLLVM Native Obfuscation
 * Same native code as Level 4 but compiled with OLLVM passes:
 * - Control flow flattening (-fla)
 * - Instruction substitution (-sub)
 * - Bogus control flow (-bcf)
 * - String obfuscation (-sobf)
 *
 * The .so file will be extremely difficult to analyze in IDA/Ghidra.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val resultText = TextView(this).apply {
            text = "Level 5 - OLLVM Protected"
            textSize = 18f
        }
        layout.addView(resultText)

        val input = EditText(this).apply { hint = "Enter solution" }
        layout.addView(input)

        for (i in 0..4) {
            val btn = Button(this).apply {
                text = "Challenge $i"
                setOnClickListener {
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
}
