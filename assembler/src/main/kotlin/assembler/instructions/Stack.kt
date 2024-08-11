package assembler.instructions

import assembler.*
import common.AluOperation
import common.WritableRegister

fun push(src: WritableRegister) = composeInstructions(
    aluP(WritableRegister.SP, WritableRegister.SP, AluOperation.A_MINUS_1),
    mov(WritableRegister.P to WritableRegister.SP),
    memWrite(WritableRegister.SP, src)
).also { require(src != WritableRegister.P && src != WritableRegister.SP) }

fun push(value: Macro<Int>) = AsmCommand { cfg ->
    listOf(
        set(WritableRegister.Q, value),
        push(WritableRegister.Q)
    ).applyConfig(cfg)
}

fun push(value: Int) = composeInstructions(
    set(WritableRegister.Q, value),
    push(WritableRegister.Q)
)


fun pop(dest: WritableRegister) = composeInstructions(
    memRead(WritableRegister.SP, dest),
    aluP(WritableRegister.SP, WritableRegister.SP, AluOperation.A_PLUS_1),
    mov(WritableRegister.P to WritableRegister.SP)
).also { require(dest != WritableRegister.P && dest != WritableRegister.SP) }


fun callExt(fn: String) = enter(fn, false)
fun callLocal(fn: String) = enter(fn, true)

private fun enter(fn: String, isLocal: Boolean) = AsmCommand { cfg ->
    val retAddr = Any()

    listOf(
        push(getLabel(retAddr)),
        push(WritableRegister.BP),
        mov(WritableRegister.SP to WritableRegister.BP),
        if (isLocal) localRef(fn) else importedRef(fn),
        jmp(WritableRegister.P),
        Label(retAddr)
    ).applyConfig(cfg)
}

fun ret() = composeInstructions(
    mov(WritableRegister.BP to WritableRegister.SP),
    pop(WritableRegister.BP),
    pop(WritableRegister.Q),
    jmp(WritableRegister.Q)
)
