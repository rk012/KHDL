package compiler.tokens

import kotlin.math.min

fun lexer(src: String): List<Token> {
    var unparsed = src
    val tokens = mutableListOf<Token>()

    while (unparsed.isNotBlank()) {
        val s = unparsed.substring(0..<min(unparsed.length, 2))
        val c = s[0]

        val (token: Token?, remaining: String) = run {
            when {
                c.isWhitespace() -> null to unparsed.trim()

                c.isJavaIdentifierStart() -> {
                    val word = c + unparsed.drop(1).takeWhile { it.isJavaIdentifierPart() }
                    val remaining = unparsed.drop(word.length)

                    Token.Keyword.entries.find { it.name.lowercase() == word }?.let { return@run it to remaining }

                    Token.Identifier(word) to remaining
                }

                c.isDigit() -> {
                    val numStr = unparsed.takeWhile { it.isDigit() }
                    val remaining = unparsed.drop(numStr.length)

                    Token.Literal.IntLiteral(numStr.toInt()) to remaining
                }

                else -> {
                    val symbol = (Token.Symbol.Separator.entries + Token.Symbol.Operator.entries)
                        .firstOrNull { s.startsWith(it.s) } ?: error("Unable tokenize:\n$unparsed")

                    symbol to unparsed.drop(symbol.s.length)
                }
            }
        }

        token?.let { tokens.add(it) }
        unparsed = remaining
    }

    return tokens
}