package assembler

data class AsmConfig(
    val directAddresses: Boolean = false,
    val offset: Int = 0,
    val imports: List<String> = emptyList(),
    val exportLabels: Map<String, String> = emptyMap()
)

data class AsmEvalContext(val labels: Map<Any, Int>) {
    operator fun get(key: Any) = requireNotNull(labels[key]) { "Undefined label: $key" }
}

fun assemble(config: AsmConfig, lines: List<AsmLine>): ObjectFile {
    if (config.directAddresses) check(config.imports.isEmpty()) { "Imports must use relative addresses" }

    var offset = config.offset
    val labels = mutableMapOf<Any, Int>()

    val instructions = lines.mapNotNull { line ->
        when (line) {
            is Label -> {
                labels[line.key] = offset
                null
            }
            is Command -> {
                val instruction = line.resolve(config)
                offset += instruction.size
                instruction
            }
        }
    }

    val ctx = AsmEvalContext(labels)

    val bytecode = instructions.flatMap {
        val code = it.eval(ctx)
        check(code.size == it.size) { "Instruction size mismatch" }
        code
    }

    return ObjectFile(
        config.imports,
        config.exportLabels.mapValues { (_, v) ->
            val pos = requireNotNull(labels[v]) { "Undefined exported label: $v" }
            (pos - offset).toUShort()
        },
        bytecode
    )
}
