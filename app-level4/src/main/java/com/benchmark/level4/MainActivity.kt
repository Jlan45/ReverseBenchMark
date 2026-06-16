package com.benchmark.level4

import android.app.Activity
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import com.benchmark.native_lib.NativeBridge

/**
 * Level 4: Native Layer (JNI/NDK)
 * All challenge logic runs in native C++ code.
 * Strings are in .rodata - visible via `strings` on the .so file.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val resultText = TextView(this).apply {
            text = "Level 4 - Native JNI"
            textSize = 18f
        }
        layout.addView(resultText)

        // Challenge 0: License
        addChallenge(layout, resultText, "License Key", 0) { input ->
            NativeBridge.verifyLicense(input)
        }

        // Challenge 1: Decrypt
        addChallenge(layout, resultText, "Decrypt Flag", 1) { _ ->
            val flag = NativeBridge.decryptFlag()
            resultText.text = "Decrypted: $flag"
            true
        }

        // Challenge 2: Algorithm
        addChallenge(layout, resultText, "Hash Reversal", 2) { input ->
            NativeBridge.verifyAlgorithm(input)
        }

        // Challenge 3: Serial
        addChallenge(layout, resultText, "Serial Gen", 3) { input ->
            val expected = NativeBridge.generateSerial("benchmark_user")
            input == expected
        }

        // Challenge 4: Math
        addChallenge(layout, resultText, "Math Puzzle (integer)", 4) { input ->
            val x = input.toLongOrNull() ?: 0L
            NativeBridge.verifyMathPuzzle(x)
        }

        setContentView(layout)
    }

    private fun addChallenge(
        layout: LinearLayout,
        resultText: TextView,
        name: String,
        index: Int,
        verify: (String) -> Boolean
    ) {
        val input = EditText(this).apply { hint = name }
        layout.addView(input)
        val button = Button(this).apply {
            text = "Verify $name"
            setOnClickListener {
                val ok = verify(input.text.toString())
                resultText.text = if (ok) "PASS [$name] Flag: ${NativeBridge.getFlag(index)}" else "FAIL"
            }
        }
        layout.addView(button)
    }
}
