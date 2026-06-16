package com.benchmark.level2

import android.app.Activity
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import com.benchmark.core.ChallengeRegistry

/**
 * Level 2: String Encryption + R8
 * All string literals are encrypted at compile time.
 * The StringDecryptor class handles runtime decryption.
 *
 * In a real build, the buildSrc StringEncryptionPlugin transforms
 * all LDC instructions to use StringDecryptor.decrypt().
 * Here we demonstrate the pattern manually for the key strings.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Encrypted: "Reverse Benchmark - Level 2 (String Encryption)"
        val titleEnc = intArrayOf(
            0x19, 0x56, 0x3B, 0x3C, 0x20, 0x43, 0x37, 0x1F,
            0x28, 0x56, 0x2E, 0x3E, 0x30, 0x3C, 0x29, 0x4E
        )
        val resultText = TextView(this).apply {
            text = StringDecryptor.decrypt(titleEnc)
            textSize = 18f
        }
        layout.addView(resultText)

        val challenges = ChallengeRegistry.getAllChallenges()
        for (challenge in challenges) {
            val label = TextView(this).apply {
                text = "[${challenge.id}]"
                textSize = 14f
            }
            layout.addView(label)

            val input = EditText(this).apply {
                hint = "Solution"
            }
            layout.addView(input)

            val button = Button(this).apply {
                text = "Verify"
                setOnClickListener {
                    val result = challenge.verify(input.text.toString())
                    resultText.text = if (result) "OK" else "FAIL"
                }
            }
            layout.addView(button)
        }

        setContentView(layout)
    }
}
