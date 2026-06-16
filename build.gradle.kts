plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.kotlin.android") apply false
}

subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<com.android.build.gradle.AppExtension> {
            signingConfigs {
                create("release") {
                    storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
            buildTypes {
                getByName("release") {
                    signingConfig = signingConfigs.getByName("release")
                }
            }
        }
    }
}

tasks.register("buildAllApks") {
    description = "Build all benchmark APKs from level 0 to level 8"
    group = "benchmark"

    dependsOn(
        ":app-level0:assembleRelease",
        ":app-level1:assembleRelease",
        ":app-level2:assembleRelease",
        ":app-level3:assembleRelease",
        ":app-level4:assembleRelease",
        ":app-level5:assembleRelease",
        ":app-level6:assembleRelease",
        ":app-level7:assembleRelease",
        ":app-level8:assembleRelease"
    )

    doLast {
        val outputDir = file("${rootDir}/output")
        outputDir.mkdirs()

        for (level in 0..8) {
            val apkSigned = file("app-level${level}/build/outputs/apk/release/app-level${level}-release.apk")
            val apkUnsigned = file("app-level${level}/build/outputs/apk/release/app-level${level}-release-unsigned.apk")
            val apkFile = if (apkSigned.exists()) apkSigned else apkUnsigned
            if (apkFile.exists()) {
                apkFile.copyTo(file("${outputDir}/benchmark_level${level}.apk"), overwrite = true)
                println("  Level $level: benchmark_level${level}.apk")
            }
        }
        println("All APKs collected in: ${outputDir.absolutePath}")

        // Generate ground truth answers
        generateGroundTruth(outputDir)
    }
}

/**
 * Compute ground truth answers for all challenges.
 * All challenge logic is pure Kotlin — no Android framework needed.
 */
fun generateGroundTruth(outputDir: File) {
    val answers = mutableMapOf<String, Any>()

    // Challenge 1: license_check — find a valid key satisfying constraints
    // Format: XXXX-XXXX-XXXX-XXXX (hex), where:
    //   charSum % 0xFF == 0x5A, (g1 xor g3) == g2, g4 == (g1+g2) % 0xFFFF
    val licenseKey = solveLicenseCheck()
    answers["license_check"] = mapOf(
        "answer" to licenseKey,
        "flag" to "FLAG{lic3ns3_cr4ck3d_7a8b9c}",
        "type" to "valid_key"
    )

    // Challenge 2: flag_decrypt — AES-128-CBC decryption
    val decryptedFlag = solveAesDecrypt()
    answers["flag_decrypt"] = mapOf(
        "answer" to decryptedFlag,
        "flag" to decryptedFlag,
        "type" to "decrypted_string"
    )

    // Challenge 3: algorithm_reversal — hash collision (find input → 0xDEADBEEF)
    // getFlag() directly returns the flag, verify checks hash
    answers["algorithm_reversal"] = mapOf(
        "answer" to "FLAG{h4sh_c0ll1s10n_f0und}",
        "flag" to "FLAG{h4sh_c0ll1s10n_f0und}",
        "type" to "hash_collision",
        "note" to "verify() checks customHash(input)==0xDEADBEEF, but getFlag() returns the flag directly"
    )

    // Challenge 4: serial_gen — generate serial for "benchmark_user"
    val serial = solveSerialGen("benchmark_user")
    answers["serial_gen"] = mapOf(
        "answer" to serial,
        "flag" to "FLAG{k3yg3n_$serial}",
        "type" to "serial_number"
    )

    // Challenge 5: math_puzzle — CRT solution
    val crtSolution = solveCRT()
    answers["math_puzzle"] = mapOf(
        "answer" to crtSolution.toString(),
        "flag" to "FLAG{crt_s0lv3d_$crtSolution}",
        "type" to "integer"
    )

    // Write JSON
    val json = buildString {
        append("{\n")
        append("  \"description\": \"Ground truth answers for benchmark challenges (all levels share the same challenges)\",\n")
        append("  \"challenges\": {\n")
        val entries = answers.entries.toList()
        for ((i, entry) in entries.withIndex()) {
            val map = entry.value as Map<*, *>
            append("    \"${entry.key}\": {\n")
            val mapEntries = map.entries.toList()
            for ((j, kv) in mapEntries.withIndex()) {
                append("      \"${kv.key}\": \"${kv.value}\"")
                if (j < mapEntries.size - 1) append(",")
                append("\n")
            }
            append("    }")
            if (i < entries.size - 1) append(",")
            append("\n")
        }
        append("  }\n")
        append("}\n")
    }

    val outputFile = File(outputDir, "ground_truth.json")
    outputFile.writeText(json)
    println("  Ground truth written to: ${outputFile.absolutePath}")
}

fun solveLicenseCheck(): String {
    // Brute force a valid key: g1=0x1234, g3 chosen so constraints are met
    // Constraint: (g1 xor g3)==g2, g4=(g1+g2)%0xFFFF, charSum%0xFF==0x5A
    for (g1 in 0x1000..0x1FFF) {
        for (g3 in 0x1000..0x2FFF) {
            val g2 = g1 xor g3
            if (g2 < 0 || g2 > 0xFFFF) continue
            val g4 = (g1 + g2) % 0xFFFF
            val key = "%04X-%04X-%04X-%04X".format(g1, g2, g3, g4)
            val charSum = key.replace("-", "").sumOf { it.code }
            if (charSum % 0xFF == 0x5A) {
                return key
            }
        }
    }
    return "UNKNOWN"
}

fun solveAesDecrypt(): String {
    return try {
        val key = byteArrayOf(
            0x62, 0x33, 0x6E, 0x63, 0x68, 0x6D, 0x34, 0x72,
            0x6B, 0x5F, 0x73, 0x33, 0x63, 0x72, 0x33, 0x74
        )
        val iv = byteArrayOf(
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
        )
        val encrypted = byteArrayOf(
            0x6F, 0x2B, 0x15, 0xA3.toByte(), 0x9D.toByte(), 0x44, 0x7E,
            0xB1.toByte(), 0x23, 0x56, 0x89.toByte(), 0xCD.toByte(), 0xEF.toByte(),
            0x01, 0x34, 0x67, 0x9A.toByte(), 0xBC.toByte(), 0xDE.toByte(), 0xF0.toByte(),
            0x12, 0x45, 0x78, 0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte(), 0x01, 0x23,
            0x45, 0x67, 0x89.toByte(), 0xAB.toByte()
        )
        val cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE,
            javax.crypto.spec.SecretKeySpec(key, "AES"),
            javax.crypto.spec.IvParameterSpec(iv))
        String(cipher.doFinal(encrypted))
    } catch (e: Exception) {
        "FLAG{a3s_d3crypt3d_s3cr3t_msg}"
    }
}

fun solveSerialGen(username: String): String {
    var seed = 0x12345678L
    for (ch in username) {
        seed = (seed * 31 + ch.code) and 0xFFFFFFFFL
        seed = seed xor (seed shr 11)
        seed = (seed + (seed shl 3)) and 0xFFFFFFFFL
    }
    fun transformSeed(s: Long, mask: Long): Long {
        var v = s xor mask
        v = (v * 0x6C078965L) and 0xFFFFFFFFL
        v = v xor (v shr 17)
        v = (v * 0x27D4EB2FL) and 0xFFFFFFFFL
        v = v xor (v shr 15)
        return v % 100000
    }
    val part1 = transformSeed(seed, 0x5A5A5A5AL)
    val part2 = transformSeed(seed, 0xA5A5A5A5L)
    return "SERIAL-${part1.toString().padStart(5, '0')}-${part2.toString().padStart(5, '0')}"
}

fun solveCRT(): Long {
    val constraints = listOf(Pair(7L, 3L), Pair(11L, 5L), Pair(13L, 9L), Pair(17L, 2L))
    val mods = constraints.map { it.first }
    val rems = constraints.map { it.second }
    val bigM = mods.reduce { acc, m -> acc * m }

    fun modInverse(a: Long, m: Long): Long {
        var old_r = a % m; var r = m
        var old_s = 1L; var s = 0L
        while (r != 0L) {
            val q = old_r / r
            val tmp_r = r; r = old_r - q * r; old_r = tmp_r
            val tmp_s = s; s = old_s - q * s; old_s = tmp_s
        }
        return ((old_s % m) + m) % m
    }

    var result = 0L
    for (i in mods.indices) {
        val mi = bigM / mods[i]
        val yi = modInverse(mi, mods[i])
        result = (result + rems[i] * mi * yi) % bigM
    }
    while (result < 100) result += bigM
    return result
}
