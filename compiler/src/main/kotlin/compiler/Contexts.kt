package compiler

import assembler.AsmBuilderScope
import assembler.Assembly
import assembler.asm
import assembler.instructions.*
import common.AluOperation
import common.JumpCondition
import common.WritableRegister
import compiler.ast.Type
import kotlin.math.min

class CompilerContext(
    val imports: Set<String>,
    val exports: Set<String>
) {
    private val loadedFunctions = mutableSetOf<CompiledFunction>()
    var scope: LexicalScope? = null
        private set

    private val breakLabels = mutableListOf<Any>()
    private val continueLabels = mutableListOf<Any>()

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

    fun AsmBuilderScope.newScope() {
        scope?.rStack?.rsave()
        scope = LexicalScope(scope)
    }

    fun AsmBuilderScope.exitScope() {
        val oldScope = requireNotNull(scope)
        require(oldScope.rStack.rSize == 0)
        oldScope.dealloc()
        scope = oldScope.parent
        scope?.rStack?.rrestore()
    }

    fun pushBreakContLabel(breakLabel: Any, contLabel: Any) {
        breakLabels.add(breakLabel)
        continueLabels.add(contLabel)
    }

    fun popBreakContLabel() {
        breakLabels.removeLast()
        continueLabels.removeLast()
    }

    fun AsmBuilderScope.jmpBreak() {
        +localRef(breakLabels.last())
        +jmp(WritableRegister.P)
    }

    fun AsmBuilderScope.jmpCont() {
        +localRef(continueLabels.last())
        +jmp(WritableRegister.P)
    }
}

class LexicalScope(val parent: LexicalScope? = null) {
    val rStack = RegisterStack()

    private val offsets = mutableMapOf<String, Int>()
    private var size = 0
    private val totalSize: Int
        get() = size + (parent?.totalSize ?: 0)

    private fun getOffset(name: String): Int? = offsets[name] ?: parent?.getOffset(name)

    fun loadArgs(args: List<Pair<Type, String>>) {
        var curOffset = -2

        args.forEach { (type, name) ->
            offsets[name] = curOffset
            curOffset -= type.wordSize
        }
    }

    context(AsmBuilderScope)
    fun newVar(name: String, varSize: Int) {
        require(name !in offsets)
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

    context(AsmBuilderScope)
    fun dealloc() {
        +set(WritableRegister.Q, size)
        +aluQ(WritableRegister.SP, WritableRegister.Q, AluOperation.A_PLUS_B)
        +mov(WritableRegister.Q to WritableRegister.SP)
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
    fun rsave() {
        (0..<min(4, rSize)).map { registers[(sp+4-it) % 4] }.reversed().forEach {
            +push(it)
        }
    }

    context(AsmBuilderScope)
    fun rrestore(argc: Int? = null) {
        if (argc != null) {
            rSize -= argc
            rSize += 1

            if (argc > 0) {
                +set(WritableRegister.P, argc)
                +aluP(WritableRegister.SP, WritableRegister.P, AluOperation.A_PLUS_B)
                +mov(WritableRegister.P to WritableRegister.SP)
            }

            +mov(WritableRegister.A to r0)
        }

        ((if (argc == null) 0 else 1)..<min(4, rSize)).map { registers[(sp+4-it) % 4] }.forEach {
            +pop(it)
        }
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
