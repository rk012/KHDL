package compiler.ast

import compiler.tokens.Token

private val matchingSymbols = mapOf(
    Token.Symbol.OPEN_BRACE to Token.Symbol.CLOSE_BRACE,
    Token.Symbol.OPEN_PAREN to Token.Symbol.CLOSE_PAREN,
)

private fun getBlock(tokens: List<Token>): List<Token> {
    val stack = mutableListOf(matchingSymbols[tokens.first()] ?: error("Expected block start"))

    return tokens.drop(1).takeWhile {
        if (it is Token.Symbol) {
            if (it in matchingSymbols.keys) {
                stack.add(matchingSymbols[it]!!)
            }

            if (it in matchingSymbols.values) {
                if (stack.isEmpty() || stack.removeLast() != it) {
                    error("Mismatched symbol: ${it.c}")
                }
            }
        }

        stack.isNotEmpty()
    }
}


private data class ParseResult<out T>(val node: T, val remaining: List<Token>)

private fun parseExpression(tokens: List<Token>): Expression {
    val literal = tokens.first() as? Token.Literal.IntLiteral ?: error("Expected integer literal")
    return Expression.Literal(literal)
}

private fun parseStatements(block: List<Token>): List<Statement> {
    val statements = mutableListOf<Statement>()
    var remaining = block

    while (remaining.isNotEmpty()) {
        val line = remaining.takeWhile { it != Token.Symbol.SEMICOLON }
        remaining = remaining.drop(line.size + 1)

        if (line.isEmpty()) continue

        when {
            line.first() == Token.Keyword.RETURN -> {
                statements.add(Statement.Return(parseExpression(line.drop(1))))
            }

            else -> error("Unexpected token: ${line.first()}")
        }
    }

    return statements
}


private fun parseFunctionDeclaration(tokens: List<Token>): ParseResult<FunctionDeclaration> {
    val returnType = run {
        val identifier = tokens.first() as? Token.Identifier ?: error("Expected identifier, got ${tokens.first()}")

        Type.Primitive.entries.find { it.name.lowercase() == identifier.value } ?: error("Unknown type: ${identifier.value}")
    }

    val name = tokens[1] as? Token.Identifier ?: error("Expected function name")

    require(tokens[2] == Token.Symbol.OPEN_PAREN) { "Expected open parenthesis" }
    require(tokens[3] == Token.Symbol.CLOSE_PAREN) { "Expected close parenthesis" }

    require(tokens[4] == Token.Symbol.OPEN_BRACE) { "Expected open brace" }

    val block = getBlock(tokens.drop(4))
    val statements = parseStatements(block)

    return ParseResult(
        FunctionDeclaration(
            Function(returnType, name.value),
            statements
        ),
        tokens.drop(block.size + 6)
    )
}


fun parse(tokens: List<Token>): SourceNode {
    var remaining = tokens

    val functions = mutableListOf<FunctionDeclaration>()

    while (remaining.isNotEmpty()) {
        val (function, newRemaining) = parseFunctionDeclaration(remaining)
        functions.add(function)
        remaining = newRemaining
    }

    return SourceNode(functions)
}
