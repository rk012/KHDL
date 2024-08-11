package assembler

import common.Bytecode
import common.CpuInstruction

fun interface AsmCommand {
    fun resolve(config: AsmConfig): List<AsmLine>
}

sealed interface AsmLine : AsmCommand {
    override fun resolve(config: AsmConfig) = listOf(this)
}

data class Label(val key: Any) : AsmLine

interface AsmInstruction : AsmLine {
    val size: Int
    fun eval(context: AsmEvalContext): Bytecode
}

inline fun AsmInstruction(size: Int, crossinline evalFn: (AsmEvalContext) -> Bytecode) = object : AsmInstruction {
    override val size = size
    override fun eval(context: AsmEvalContext) = evalFn(context)
}

fun composeInstructions(vararg instructions: AsmInstruction) = object : AsmInstruction {
    override val size = instructions.sumOf { it.size }
    override fun eval(context: AsmEvalContext) = instructions.flatMap { it.eval(context) }
}

fun List<AsmCommand>.applyConfig(config: AsmConfig) = flatMap { it.resolve(config) }

fun cpuInstructions(vararg instructions: CpuInstruction) = AsmInstruction(instructions.size) { _ ->
    instructions.map(CpuInstruction::code)
}
