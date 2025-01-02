package compiler

import compiler.ast.*
import compiler.tokens.Token

class AddSub : IntProgramTest(false) {
    // TODO add mult/div test here
    override val sourceCode = """
        int main() {
            return (1+2) - (3-4);
        }
    """.trimIndent()

    override val expectedTokens = listOf(
        Token.Identifier("int"),
        Token.Identifier("main"),
        Token.Symbol.Separator.OPEN_PAREN,
        Token.Symbol.Separator.CLOSE_PAREN,
        Token.Symbol.Separator.OPEN_BRACE,

        Token.Keyword.RETURN,

        Token.Symbol.Separator.OPEN_PAREN,
        Token.Literal.IntLiteral(1),
        Token.Symbol.Operator.PLUS,
        Token.Literal.IntLiteral(2),
        Token.Symbol.Separator.CLOSE_PAREN,
        Token.Symbol.Operator.MINUS,
        Token.Symbol.Separator.OPEN_PAREN,
        Token.Literal.IntLiteral(3),
        Token.Symbol.Operator.MINUS,
        Token.Literal.IntLiteral(4),
        Token.Symbol.Separator.CLOSE_PAREN,


        Token.Symbol.Separator.SEMICOLON,

        Token.Symbol.Separator.CLOSE_BRACE,
    )

    override val expectedTree = SourceNode(listOf(
        GlobalElement.FunctionDefinition(
            Function(Type.Primitive.INT, "main", emptyList()),
            Statement.Block(listOf(
                Statement.Return(
                    Expression.Binary.Subtract(
                        Expression.Binary.Add(
                            Expression.Literal(Token.Literal.IntLiteral(1)),
                            Expression.Literal(Token.Literal.IntLiteral(2))
                        ),
                        Expression.Binary.Subtract(
                            Expression.Literal(Token.Literal.IntLiteral(3)),
                            Expression.Literal(Token.Literal.IntLiteral(4))
                        )
                    )
                )
            ))
        )
    ))

    override val expectedReturn = 4
}