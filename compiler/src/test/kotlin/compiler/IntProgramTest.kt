package compiler

import VirtualMachine
import assembler.AsmConfig
import assembler.asm
import assembler.assemble
import assembler.instructions.callLocal
import assembler.instructions.hlt
import assembler.instructions.mov
import assembler.instructions.set
import assembler.link
import common.WritableRegister
import compiler.ast.SourceNode
import compiler.tokens.Token
import compiler.tokens.lexer
import org.junit.jupiter.api.condition.DisabledIf
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class IntProgramTest(
    @Suppress("MemberVisibilityCanBePrivate") protected val isReturnOnly: Boolean = true
) {
    abstract val sourceCode: String
    open val expectedTokens: List<Token>? = null
    open val expectedTree: SourceNode? = null
    abstract val expectedReturn: Int

    @Test
    @DisabledIf("compiler.IntProgramTest#isReturnOnly")
    fun tokens() {
        if (expectedTokens == null) return
        assertEquals(expectedTokens, lexer(sourceCode))
    }

    @Test
    @DisabledIf("compiler.IntProgramTest#isReturnOnly")
    fun ast() {
        if (expectedTree == null) return
        assertEquals(expectedTree, parseTokens(lexer(sourceCode)))
    }

    @Test
    fun executed() {
        val compiled = compileSource(sourceCode)

        val executable = asm {
            +set(WritableRegister.SP, 0)
            +mov(WritableRegister.SP to WritableRegister.BP)
            +callLocal("__fn_main")
            +hlt()

            addAll(compiled)
        }.let {
            link(null, assemble(AsmConfig(), it))
        }

        val vm = VirtualMachine(executable.bytecode)
        vm.runUntilHalt()
        assertEquals(expectedReturn.toShort(), vm.debugRegister(WritableRegister.A))
    }
}