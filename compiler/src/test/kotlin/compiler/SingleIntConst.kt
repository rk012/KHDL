package compiler

import compiler.ast.*
import compiler.ast.Function
import compiler.tokens.Token

class SingleIntConst : IntProgramTest(false) {
    override val sourceCode = """
        int main() {
            return 5;
        }
    """.trimIndent()

    override val expectedTokens = listOf(
        Token.Identifier("int"),
        Token.Identifier("main"),
        Token.Symbol.Separator.OPEN_PAREN,
        Token.Symbol.Separator.CLOSE_PAREN,
        Token.Symbol.Separator.OPEN_BRACE,

        Token.Keyword.RETURN,
        Token.Literal.IntLiteral(5),
        Token.Symbol.Separator.SEMICOLON,

        Token.Symbol.Separator.CLOSE_BRACE,
    )

    override val expectedTree = SourceNode(listOf(
        FunctionDeclaration(
            Function(Type.Primitive.INT, "main"),
            listOf(
                Statement.Return(Expression.Literal(Token.Literal.IntLiteral(5)))
            )
        )
    ))

    override val expectedReturn = 5
}