package compiler

import compiler.ast.*
import compiler.ast.Function
import compiler.tokens.Token

class SingleIntConst : IntProgramTest {
    override val sourceCode = """
        int main() {
            return 5;
        }
    """.trimIndent()

    override val expectedTokens = listOf(
        Token.Identifier("int"),
        Token.Identifier("main"),
        Token.Symbol.OPEN_PAREN,
        Token.Symbol.CLOSE_PAREN,
        Token.Symbol.OPEN_BRACE,

        Token.Keyword.RETURN,
        Token.Literal.IntLiteral(5),
        Token.Symbol.SEMICOLON,

        Token.Symbol.CLOSE_BRACE,
    )

    override val expectedTree = SourceNode(listOf(
        FunctionDeclaration(
            Function(Type.Primitive.INT, "main"),
            listOf(
                Statement.Return(Expression.Literal(Token.Literal.IntLiteral(5)))
            )
        )
    ))
}