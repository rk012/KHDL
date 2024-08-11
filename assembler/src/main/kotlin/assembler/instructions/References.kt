package assembler.instructions

import assembler.*
import common.AluOperation
import common.ReadOnlyRegister
import common.WritableRegister

fun localRef(target: String) = AsmCommand { cfg ->
    if (cfg.directAddresses) return@AsmCommand set(WritableRegister.P, getLabel(target)).resolve(cfg)

    val ip = Any()

    listOf(
        set(WritableRegister.P, macro { getLabel(target).bind() - getLabel(ip).bind() }),
        mov(ReadOnlyRegister.IP to WritableRegister.Q),
        Label(ip),
        aluP(WritableRegister.Q, WritableRegister.P, AluOperation.A_PLUS_B)
    ).applyConfig(cfg)
}

fun importedRef(target: String) = AsmCommand { cfg ->
    require(target in cfg.imports) { "Unknown import: $target" }
    val pos = cfg.offset + cfg.imports.run { indexOf(target) - size }

    val ip = Any()

    listOf(
        set(WritableRegister.P, macro { pos - getLabel(ip).bind() }),
        mov(ReadOnlyRegister.IP to WritableRegister.Q),
        Label(ip),
        aluP(WritableRegister.Q, WritableRegister.P, AluOperation.A_PLUS_B),
        memRead(WritableRegister.P, WritableRegister.Q),
        aluP(WritableRegister.P, WritableRegister.Q, AluOperation.A_PLUS_B)
    ).applyConfig(cfg)
}
