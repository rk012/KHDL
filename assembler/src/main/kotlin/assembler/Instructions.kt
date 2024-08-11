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

interface InstructionBlock : AsmLine {
    val size: Int
    fun eval(context: AsmEvalContext): Bytecode
}

inline fun InstructionBlock(size: Int, crossinline evalFn: (AsmEvalContext) -> Bytecode) = object : InstructionBlock {
    override val size = size
    override fun eval(context: AsmEvalContext) = evalFn(context)
}

fun composeCommands(vararg commands: AsmCommand) = AsmCommand { cfg ->
    commands.flatMap { it.resolve(cfg) }
}

fun List<AsmCommand>.applyConfig(config: AsmConfig) = flatMap { it.resolve(config) }

fun cpuInstructions(vararg instructions: CpuInstruction) = InstructionBlock(instructions.size) { _ ->
    instructions.map(CpuInstruction::code)
}
