package com.benchmark.level3

import android.app.Activity
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import com.benchmark.core.ChallengeRegistry

/**
 * Level 3: Control Flow Obfuscation + String Encryption + R8
 * Challenge logic is wrapped in control-flow-flattened state machines
 * with opaque predicates and bogus basic blocks.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val resultText = TextView(this).apply {
            text = decryptString(intArrayOf(0x19, 0x56, 0x3B, 0x3C, 0x20, 0x43))
            textSize = 18f
        }
        layout.addView(resultText)

        val challenges = ChallengeRegistry.getAllChallenges()
        for ((index, challenge) in challenges.withIndex()) {
            val input = EditText(this).apply { hint = "Challenge $index" }
            layout.addView(input)

            val button = Button(this).apply {
                text = "Go"
                setOnClickListener {
                    // Use both the original challenge verify and the flattened engine
                    val r1 = challenge.verify(input.text.toString())
                    val r2 = ControlFlowEngine.flattenedVerify(input.text.toString(), index)
                    resultText.text = if (r1) "PASS" else "FAIL"
                }
            }
            layout.addView(button)
        }

        setContentView(layout)
    }

    private fun decryptString(enc: IntArray): String {
        val key = byteArrayOf(0x4B, 0x33, 0x59, 0x5F, 0x46, 0x30, 0x52, 0x5F)
        val result = ByteArray(enc.size)
        for (i in enc.indices) {
            result[i] = (enc[i] xor key[i % key.size].toInt()).toByte()
        }
        return String(result, Charsets.UTF_8)
    }
}
