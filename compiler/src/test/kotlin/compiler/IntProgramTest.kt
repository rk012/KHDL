package compiler

import compiler.ast.SourceNode
import compiler.ast.parse
import compiler.tokens.Token
import compiler.tokens.lexer
import kotlin.test.Test
import kotlin.test.assertEquals

interface IntProgramTest {
    val sourceCode: String
    val expectedTokens: List<Token>
    val expectedTree: SourceNode

    @Test
    fun tokens() {
        assertEquals(expectedTokens, lexer(sourceCode))
    }

    @Test
    fun ast() {
        assertEquals(expectedTree, parse(lexer(sourceCode)))
    }
}