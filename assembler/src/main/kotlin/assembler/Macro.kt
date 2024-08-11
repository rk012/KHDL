package assembler

fun interface Macro<out T> {
    fun eval(config: AsmConfig, context: AsmEvalContext): T
}

class MacroScope internal constructor(val cfg: AsmConfig, val ctx: AsmEvalContext) {
    fun <T> Macro<T>.bind(): T = this@bind.eval(cfg, ctx)
}

fun <T> macro(block: MacroScope.() -> T): Macro<T> =
    Macro { cfg, ctx -> MacroScope(cfg, ctx).block() }


fun getLabel(label: Any) = macro { ctx[label] }
