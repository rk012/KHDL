package compiler

import assembler.AsmConfig
import assembler.asm
import assembler.assemble
import assembler.instructions.*
import common.WritableRegister

sealed interface CRuntime : CompiledFunction {
    fun compiled() = asm {
        +"__fn_$name"
        addAll(assembly)
    }.let { assemble(AsmConfig(imports = listOf("main"), exportLabels = mapOf(name to "__fn_${name}")), it) }

    data object CRT0 : CRuntime {
        override val name = "__crt0_init"

        override val assembly = asm {
            +set(WritableRegister.SP, 0)
            +mov(WritableRegister.SP to WritableRegister.BP)
            +callExt("main")
            +hlt()
        }
    }

    data object CRT1 : CRuntime {
        override val name = "__crt1_init"

        override val assembly = asm {
            +callExt("main")
            +ret()
        }
    }
}