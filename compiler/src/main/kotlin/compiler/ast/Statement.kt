package compiler.ast

import compiler.tokens.Token

sealed interface Statement {
    data class Expr(val expression: Expression) : Statement {
        companion object : Parser<Expr> by parser ({
            Expr(Expression.parse()).also { match(Token.Symbol.Separator.SEMICOLON) }
        })
    }

    data class Declaration(val type: Type, val name: String, val initializer: Expression?) : Statement {
        companion object : Parser<Declaration> by parser ({
            val type = Type.parse()
            val name = match<Token.Identifier>().value
            var initializer: Expression? = null

            if (peek() == Token.Symbol.Operator.ASSIGN) {
                match(Token.Symbol.Operator.ASSIGN)
                initializer = Expression.parse()
            }

            Declaration(type, name, initializer).also { match(Token.Symbol.Separator.SEMICOLON) }
        })
    }

    data class Return(val expression: Expression) : Statement {
        companion object : Parser<Return> by parser ({
            match(Token.Keyword.RETURN)

            Return(Expression.parse()).also { match(Token.Symbol.Separator.SEMICOLON) }
        })
    }

    companion object : Parser<Statement> by parseAny(
        Expr,
        Declaration,
        Return
    )
}

private val matchingSymbols = mapOf(
    Token.Symbol.Separator.OPEN_BRACE to Token.Symbol.Separator.CLOSE_BRACE,
    Token.Symbol.Separator.OPEN_PAREN to Token.Symbol.Separator.CLOSE_PAREN,
)

fun blockParser(blockType: Token.Symbol.Separator): Parser<List<Token>> {
    require(blockType in matchingSymbols.keys)

    return parser {
        match(blockType)

        val stack = mutableListOf(matchingSymbols[blockType]!!)
        val tokens = mutableListOf<Token>()

        while (true) {
            val token = next()

            if (token is Token.Symbol.Separator) {
                if (token in matchingSymbols.keys) {
                    stack.add(matchingSymbols[token]!!)
                }

                if (token in matchingSymbols.values) {
                    if (stack.isEmpty() || stack.removeLast() != token) {
                        raise("Mismatched symbol: ${token.s}")
                    }
                }
            }

            if (stack.isEmpty()) break
            tokens.add(token)
        }

        tokens
    }
}

val statementBlockParser = parser<List<Statement>> {
    blockParser(Token.Symbol.Separator.OPEN_BRACE).parse().parseWith(parser {
        val statements = mutableListOf<Statement>()

        while (peek() != null) {
            statements.add(Statement.parse())
        }

        statements
    })
}
