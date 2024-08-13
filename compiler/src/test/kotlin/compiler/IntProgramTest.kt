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
import compiler.parser.parseTokens
import kotlin.test.Test
import kotlin.test.assertEquals

interface IntProgramTest {
    val sourceCode: String
    val expectedTokens: List<Token>
    val expectedTree: SourceNode
    val expectedReturn: Int

    @Test
    fun tokens() {
        assertEquals(expectedTokens, lexer(sourceCode))
    }

    @Test
    fun ast() {
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