package compiler.tokens

fun lexer(src: String): List<Token> {
    var unparsed = src
    val tokens = mutableListOf<Token>()

    while (unparsed.isNotBlank()) {
        val c = unparsed.first()

        val (token: Token?, remaining: String) = run {
            when {
                c.isWhitespace() -> null to unparsed.trim()

                c.isLetter() -> {
                    val word = unparsed.takeWhile { it.isLetter() }
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
                    val symbol = Token.Symbol.entries.find { it.c == c } ?: error("Unable tokenize:\n$unparsed")

                    symbol to unparsed.drop(1)
                }
            }
        }

        token?.let { tokens.add(it) }
        unparsed = remaining
    }

    return tokens
}