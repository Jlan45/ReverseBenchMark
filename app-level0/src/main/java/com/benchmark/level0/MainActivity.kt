package com.benchmark.level0

import android.app.Activity
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import com.benchmark.core.ChallengeRegistry

/**
 * Level 0: No protection at all.
 * Plain readable code - serves as baseline for AI reverse engineering.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val resultText = TextView(this).apply {
            text = "Reverse Benchmark - Level 0 (No Protection)"
            textSize = 18f
        }
        layout.addView(resultText)

        val challenges = ChallengeRegistry.getAllChallenges()
        for (challenge in challenges) {
            val label = TextView(this).apply {
                text = "\n[${challenge.id}] ${challenge.description}"
                textSize = 14f
            }
            layout.addView(label)

            val input = EditText(this).apply {
                hint = "Enter solution for ${challenge.id}"
            }
            layout.addView(input)

            val button = Button(this).apply {
                text = "Verify"
                setOnClickListener {
                    val result = challenge.verify(input.text.toString())
                    resultText.text = if (result) "CORRECT! Flag: ${challenge.getFlag()}" else "WRONG"
                }
            }
            layout.addView(button)
        }

        setContentView(layout)
    }
}
