package compiler.ast

import compiler.parser.Parser
import compiler.parser.match
import compiler.parser.parser
import compiler.tokens.Token

sealed interface Statement {
    data class Return(val expression: Expression) : Statement

    companion object : Parser<Statement> by parser({
        match(Token.Keyword.RETURN)

        Return(Expression.parse())
    })
}

private val matchingSymbols = mapOf(
    Token.Symbol.OPEN_BRACE to Token.Symbol.CLOSE_BRACE,
    Token.Symbol.OPEN_PAREN to Token.Symbol.CLOSE_PAREN,
)

fun blockParser(blockType: Token.Symbol): Parser<List<Token>> {
    require(blockType in matchingSymbols.keys)

    return parser {
        match(blockType)

        val stack = mutableListOf(matchingSymbols[blockType]!!)
        val tokens = mutableListOf<Token>()

        while (true) {
            val token = next()

            if (token is Token.Symbol) {
                if (token in matchingSymbols.keys) {
                    stack.add(matchingSymbols[token]!!)
                }

                if (token in matchingSymbols.values) {
                    if (stack.isEmpty() || stack.removeLast() != token) {
                        error("Mismatched symbol: ${token.c}")
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
    val statements = mutableListOf<Statement>()
    var remaining = blockParser(Token.Symbol.OPEN_BRACE).parse()

    while (remaining.isNotEmpty()) {
        val line = remaining.takeWhile { it != Token.Symbol.SEMICOLON }
        remaining = remaining.drop(line.size + 1)

        if (line.isEmpty()) continue
        statements.add(line.parseWith(Statement))
    }

    statements
}
