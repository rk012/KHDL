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
    val scope = LexicalScope()

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

class LexicalScope(val parent: LexicalScope? = null) {
    val rStack = RegisterStack()

    private val offsets = mutableMapOf<String, Int>()
    private var size = 0
    private val totalSize: Int
        get() = size + (parent?.totalSize ?: 0)

    private fun getOffset(name: String): Int? = offsets[name] ?: parent?.getOffset(name)

    context(AsmBuilderScope)
    fun newVar(name: String, varSize: Int) {
        size += varSize
        offsets[name] = totalSize
        if (varSize == 1) +aluQ(WritableRegister.SP, WritableRegister.SP, AluOperation.A_MINUS_1)
        else {
            +set(WritableRegister.Q, varSize)
            +aluQ(WritableRegister.SP, WritableRegister.Q, AluOperation.A_MINUS_B)
        }
        +mov(WritableRegister.Q to WritableRegister.SP)
    }

    context(AsmBuilderScope)
    fun rpushVar(name: String) {
        val offset = requireNotNull(getOffset(name)) { "Undeclared variable: $name" }
        rStack.rpush()
        +getVar(offset, rStack.r0)
    }

    context(AsmBuilderScope)
    fun rpopVar(name: String) {
        val offset = requireNotNull(getOffset(name)) { "Undeclared variable: $name" }
        +setVar(offset, rStack.r0)
        rStack.rpop()
    }
}

class RegisterStack {
    private val registers = listOf(WritableRegister.A, WritableRegister.B, WritableRegister.C, WritableRegister.D)
    var rSize = 0
        private set
    private var sp = 3

    val r0
        get() = registers[sp]

    val r1
        get() = registers[(sp + 3) % 4]

    val r2
        get() = registers[(sp + 2) % 4]

    val r3
        get() = registers[(sp + 1) % 4]

    fun rforget() {
        require(rSize > 0)
        sp = (sp + 3) % 4
        rSize--
    }

    fun rfake() {
        sp = (sp + 1) % 4
        rSize++
    }

    context(AsmBuilderScope)
    fun rpop() {
        require(rSize > 0)
        if (rSize > 4) +pop(registers[sp])
        sp = (sp + 3) % 4
        rSize--
    }

    context(AsmBuilderScope)
    fun rpush() {
        sp = (sp + 1) % 4
        if (rSize >= 4) +push(registers[sp])
        rSize++
    }

    context(AsmBuilderScope)
    fun rpushLit(x: Int) {
        rpush()
        +set(r0, x)
    }

    context(AsmBuilderScope)
    fun binOp(op: AluOperation) {
        require(rSize >= 2)
        val nsp = (sp + 3) % 4
        +aluP(registers[nsp], registers[sp], op)
        +mov(WritableRegister.P to registers[nsp])
        rpop()
    }

    context(AsmBuilderScope)
    fun uOp(op: AluOperation) {
        require(rSize >= 1)
        +aluP(registers[sp], registers[sp], op)
        +mov(WritableRegister.P to registers[sp])
    }

    context(AsmBuilderScope)
    fun uCmp(cond: JumpCondition) {
        +aluP(registers[sp], registers[sp], AluOperation.A)
        +cmp(cond, registers[sp])
    }
}
