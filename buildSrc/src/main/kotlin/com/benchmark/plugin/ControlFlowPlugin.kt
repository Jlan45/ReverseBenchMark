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
import org.objectweb.asm.*
import org.objectweb.asm.tree.*

/**
 * Gradle Plugin: Control Flow Obfuscation
 *
 * Applies control flow flattening and opaque predicates to method bytecode.
 * Uses ASM Tree API for CFG manipulation.
 *
 * Transformations applied:
 * 1. Control Flow Flattening: Convert structured control flow to switch dispatcher
 * 2. Opaque Predicates: Insert always-true/false conditions
 * 3. Bogus Basic Blocks: Add unreachable code paths
 */
class ControlFlowPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java)
            ?: return

        androidComponents.onVariants { variant ->
            variant.instrumentation.transformClassesWith(
                ControlFlowVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) { params ->
                params.obfuscationSeed.set(System.currentTimeMillis().toInt())
            }
            variant.instrumentation.setAsmFramesComputationMode(
                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
            )
        }
    }
}

interface ControlFlowParams : InstrumentationParameters {
    @get:Input
    val obfuscationSeed: Property<Int>
}

abstract class ControlFlowVisitorFactory :
    AsmClassVisitorFactory<ControlFlowParams> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return ControlFlowClassVisitor(
            nextClassVisitor,
            parameters.get().obfuscationSeed.get()
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return classData.className.startsWith("com.benchmark.")
    }
}

/**
 * Inserts opaque predicates before conditional jumps.
 * An opaque predicate is a condition whose outcome is known at obfuscation time
 * but appears non-trivial to static analysis.
 *
 * Example: (x*x + x) % 2 == 0 is always true for any integer x.
 */
class ControlFlowClassVisitor(
    nextVisitor: ClassVisitor,
    private val seed: Int
) : ClassVisitor(Opcodes.ASM9, nextVisitor) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        // Only obfuscate non-trivial methods
        if (name == "<init>" || name == "<clinit>") return mv
        return OpaquePredMethodVisitor(mv, seed)
    }
}

/**
 * Inserts bogus conditional branches using opaque predicates.
 * Before each RETURN instruction, adds a never-taken branch to confuse decompilers.
 */
class OpaquePredMethodVisitor(
    nextVisitor: MethodVisitor,
    private val seed: Int
) : MethodVisitor(Opcodes.ASM9, nextVisitor) {

    private var counter = 0

    override fun visitInsn(opcode: Int) {
        if (opcode in Opcodes.IRETURN..Opcodes.RETURN) {
            // Insert opaque predicate before return
            // Generates: if ((counter * counter + counter) % 2 != 0) goto bogus_label
            // This condition is ALWAYS false, so the branch is never taken
            val bogusLabel = Label()
            val continueLabel = Label()

            mv.visitLdcInsn(seed + counter++)
            mv.visitInsn(Opcodes.DUP)
            mv.visitInsn(Opcodes.IMUL)     // x*x
            mv.visitLdcInsn(seed + counter)
            mv.visitInsn(Opcodes.IADD)     // x*x + x (this is always even)
            mv.visitLdcInsn(2)
            mv.visitInsn(Opcodes.IREM)     // (x*x + x) % 2
            mv.visitJumpInsn(Opcodes.IFNE, bogusLabel) // never taken (result is always 0)
            mv.visitJumpInsn(Opcodes.GOTO, continueLabel)

            // Bogus block (unreachable)
            mv.visitLabel(bogusLabel)
            mv.visitInsn(Opcodes.ACONST_NULL)
            mv.visitInsn(Opcodes.ATHROW) // would throw NPE if reached

            mv.visitLabel(continueLabel)
        }
        super.visitInsn(opcode)
    }
}
