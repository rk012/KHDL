package compiler

import compiler.ast.*
import compiler.ast.Function
import compiler.tokens.Token

class UnaryOp : IntProgramTest(false) {
    override val sourceCode = """
        int main() {
            return !~-!0;
        }
    """.trimIndent()

    override val expectedTokens = listOf(
        Token.Identifier("int"),
        Token.Identifier("main"),
        Token.Symbol.Separator.OPEN_PAREN,
        Token.Symbol.Separator.CLOSE_PAREN,
        Token.Symbol.Separator.OPEN_BRACE,

        Token.Keyword.RETURN,

        Token.Symbol.Operator.BANG,
        Token.Symbol.Operator.TILDE,
        Token.Symbol.Operator.MINUS,
        Token.Symbol.Operator.BANG,
        Token.Literal.IntLiteral(0),

        Token.Symbol.Separator.SEMICOLON,

        Token.Symbol.Separator.CLOSE_BRACE,
    )

    override val expectedTree = SourceNode(listOf(
        FunctionDeclaration(
            Function(Type.Primitive.INT, "main"),
            Statement.Block(listOf(
                Statement.Return(
                    Expression.Unary.LogicalNot(
                    Expression.Unary.BitwiseNot(
                    Expression.Unary.Negate(
                    Expression.Unary.LogicalNot(
                    Expression.Literal(Token.Literal.IntLiteral(0))

                )))))
            ))
        )
    ))

    override val expectedReturn = 1
}