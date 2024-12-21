package compiler

import assembler.AsmBuilderScope
import assembler.Assembly
import assembler.asm
import assembler.instructions.*
import common.AluOperation
import common.JumpCondition
import common.WritableRegister

class CompilerContext {
    private val loadedFunctions = mutableSetOf<CompiledFunction>()
    val registerStack = RegisterStack()

    fun addCompiled(fn: CompiledFunction) {
        loadedFunctions.add(fn)
    }

    fun AsmBuilderScope.callCompiled(fn: CompiledFunction) {
        addCompiled(fn)
        +callLocal(fn.name)
    }

    fun compiledFunctions(): Assembly = asm {
        loadedFunctions.forEach { fn ->
            +fn.name
            addAll(fn.assembly)
        }
    }
}

class RegisterStack {
    private val registers = listOf(WritableRegister.A, WritableRegister.B, WritableRegister.C, WritableRegister.D)
    var rSize = 0
        private set
    private var sp = 3

    fun AsmBuilderScope.rpop() {
        require(rSize > 0)
        if (rSize > 4) +pop(registers[sp])
        sp = (sp + 3) % 4
        rSize--
    }

    fun AsmBuilderScope.rpushLit(x: Int) {
        sp = (sp + 1) % 4
        if (rSize >= 4) +push(registers[sp])
        +set(registers[sp], x)
        rSize++
    }

    fun AsmBuilderScope.binOp(op: AluOperation) {
        require(rSize >= 2)
        val nsp = (sp + 3) % 4
        +aluP(registers[nsp], registers[sp], op)
        +mov(WritableRegister.P to registers[nsp])
        rpop()
    }

    fun AsmBuilderScope.uOp(op: AluOperation) {
        require(rSize >= 1)
        +aluP(registers[sp], registers[sp], op)
        +mov(WritableRegister.P to registers[sp])
    }

    fun AsmBuilderScope.uCmp(cond: JumpCondition) {
        +aluP(registers[sp], registers[sp], AluOperation.A)
        +cmp(cond, registers[sp])
    }
}
