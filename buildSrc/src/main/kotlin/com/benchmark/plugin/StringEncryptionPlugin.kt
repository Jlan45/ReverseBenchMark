package com.benchmark.plugin

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Gradle Plugin: Compile-time String Encryption
 *
 * Transforms all LDC (string constant load) instructions in target packages
 * into calls to a runtime decryptor. Each string is XOR-encrypted with a
 * per-build random key.
 *
 * Usage in build.gradle.kts:
 *   plugins { id("benchmark.string-encryption") }
 *
 * This plugin uses AGP 8+ Instrumentation API (AsmClassVisitorFactory).
 */
class StringEncryptionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java)
            ?: return

        androidComponents.onVariants { variant ->
            variant.instrumentation.transformClassesWith(
                StringEncryptionVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) { params ->
                params.encryptionKey.set(generateRandomKey())
            }
            variant.instrumentation.setAsmFramesComputationMode(
                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
            )
        }
    }

    private fun generateRandomKey(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..16).map { chars.random() }.joinToString("")
    }
}

interface StringEncryptionParams : InstrumentationParameters {
    @get:Input
    val encryptionKey: Property<String>
}

abstract class StringEncryptionVisitorFactory :
    AsmClassVisitorFactory<StringEncryptionParams> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return StringEncryptionClassVisitor(
            nextClassVisitor,
            parameters.get().encryptionKey.get()
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        // Only instrument our benchmark classes, not Android framework
        return classData.className.startsWith("com.benchmark.")
    }
}

/**
 * ASM ClassVisitor that encrypts string constants in method bodies.
 */
class StringEncryptionClassVisitor(
    nextVisitor: ClassVisitor,
    private val key: String
) : ClassVisitor(Opcodes.ASM9, nextVisitor) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return StringEncryptionMethodVisitor(mv, key)
    }
}

/**
 * ASM MethodVisitor that replaces LDC string instructions with decryptor calls.
 *
 * Transforms:
 *   LDC "secret_string"
 * Into:
 *   LDC encrypted_bytes_array
 *   INVOKESTATIC com/benchmark/level2/StringDecryptor.decrypt([B)Ljava/lang/String;
 */
class StringEncryptionMethodVisitor(
    nextVisitor: MethodVisitor,
    private val key: String
) : MethodVisitor(Opcodes.ASM9, nextVisitor) {

    override fun visitLdcInsn(value: Any?) {
        if (value is String && value.length > 3 && shouldEncrypt(value)) {
            // Encrypt the string
            val encrypted = encryptString(value, key)

            // Push encrypted byte array
            mv.visitIntInsn(Opcodes.BIPUSH, encrypted.size)
            mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE)
            for (i in encrypted.indices) {
                mv.visitInsn(Opcodes.DUP)
                mv.visitIntInsn(Opcodes.BIPUSH, i)
                mv.visitIntInsn(Opcodes.BIPUSH, encrypted[i].toInt())
                mv.visitInsn(Opcodes.BASTORE)
            }

            // Call decryptor
            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/benchmark/level2/StringDecryptor",
                "decrypt",
                "([B)Ljava/lang/String;",
                false
            )
        } else {
            super.visitLdcInsn(value)
        }
    }

    private fun shouldEncrypt(s: String): Boolean {
        // Don't encrypt very short strings or Android system strings
        return s.length > 4 && !s.startsWith("android.") && !s.startsWith("java.")
    }

    private fun encryptString(plaintext: String, key: String): ByteArray {
        val keyBytes = key.toByteArray()
        val plainBytes = plaintext.toByteArray()
        return ByteArray(plainBytes.size) { i ->
            (plainBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
    }
}
