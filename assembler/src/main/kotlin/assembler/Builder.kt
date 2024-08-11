package assembler

class AsmBuilderScope internal constructor() {
    internal val commands = mutableListOf<AsmCommand>()

    operator fun AsmCommand.unaryPlus() {
        commands += this
    }

    operator fun String.unaryPlus() {
        commands += Label(this)
    }

    fun addAll(commands: Assembly) {
        this.commands.addAll(commands)
    }
}

fun asm(block: AsmBuilderScope.() -> Unit): Assembly {
    val scope = AsmBuilderScope()
    scope.block()
    return scope.commands
}
