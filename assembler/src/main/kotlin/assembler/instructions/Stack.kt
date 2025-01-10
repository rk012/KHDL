package assembler.instructions

import assembler.*
import common.AluOperation
import common.WritableRegister

fun push(src: WritableRegister): AsmCommand {
    require(src != WritableRegister.P && src != WritableRegister.SP)

    return composeCommands(
        aluP(WritableRegister.SP, WritableRegister.SP, AluOperation.A_MINUS_1),
        mov(WritableRegister.P to WritableRegister.SP),
        memWrite(WritableRegister.SP, src)
    )
}

fun push(value: Macro<Int>) = composeCommands(
    set(WritableRegister.Q, value),
    push(WritableRegister.Q)
)

fun push(value: Int) = composeCommands(
    set(WritableRegister.Q, value),
    push(WritableRegister.Q)
)


fun pop(dest: WritableRegister) = composeCommands(
    memRead(WritableRegister.SP, dest),
    aluP(WritableRegister.SP, WritableRegister.SP, AluOperation.A_PLUS_1),
    mov(WritableRegister.P to WritableRegister.SP)
).also { require(dest != WritableRegister.P && dest != WritableRegister.SP) }


fun lea(offset: Int) = composeCommands(
    set(WritableRegister.P, offset),
    aluP(WritableRegister.BP, WritableRegister.P, AluOperation.A_MINUS_B)
)

fun lea(offset: Int, dest: WritableRegister) =
    if (dest == WritableRegister.P) lea(offset)
    else composeCommands(
        lea(offset),
        mov(WritableRegister.P to dest)
    )

fun getVar(offset: Int, dest: WritableRegister) = composeCommands(
    lea(offset),
    memRead(WritableRegister.P, dest)
)

fun setVar(offset: Int, src: WritableRegister): AsmCommand {
    require(src != WritableRegister.P)

    return composeCommands(
        lea(offset),
        memWrite(WritableRegister.P, src)
    )
}

fun setVar(offset: Int, value: Int) = composeCommands(
    set(WritableRegister.Q, value),
    setVar(offset, WritableRegister.Q)
)

fun callExt(fn: String) = enter(fn, false)
fun callLocal(fn: String) = enter(fn, true)

private fun enter(fn: String, isLocal: Boolean): AsmCommand {
    val retAddr = Any()

    return composeCommands(
        localRef(retAddr),
        mov(WritableRegister.P to WritableRegister.Q),
        push(WritableRegister.Q),
        push(WritableRegister.BP),
        mov(WritableRegister.SP to WritableRegister.BP),
        if (isLocal) localRef(fn) else importedRef(fn),
        jmp(WritableRegister.P),
        Label(retAddr)
    )
}

fun ret() = composeCommands(
    mov(WritableRegister.BP to WritableRegister.SP),
    pop(WritableRegister.BP),
    pop(WritableRegister.Q),
    jmp(WritableRegister.Q)
)
