package assembler

import common.Bytecode

interface AsmInstruction {
    val size: Int
    fun eval(context: AsmEvalContext): Bytecode
}

inline fun AsmInstruction(size: Int, crossinline evalFn: (AsmEvalContext) -> Bytecode) = object : AsmInstruction {
    override val size = size
    override fun eval(context: AsmEvalContext) = evalFn(context)
}

sealed interface AsmLine

data class Label(val key: Any) : AsmLine
fun interface Command : AsmLine {
    fun resolve(config: AsmConfig): AsmInstruction
}


fun compose(vararg commands: Command): Command = Command { cfg ->
    val instructions = commands.map { it.resolve(cfg) }
    AsmInstruction(instructions.sumOf { it.size }) { ctx ->
        instructions.flatMap { it.eval(ctx) }
    }
}


fun interface Macro<out T> {
    fun eval(config: AsmConfig, context: AsmEvalContext): T
}
