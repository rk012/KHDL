package compiler.tokens


sealed interface Token {
    data class Identifier(val value: String) : Token

    enum class Symbol(val c: Char) : Token {
        OPEN_PAREN('('),
        CLOSE_PAREN(')'),
        OPEN_BRACE('{'),
        CLOSE_BRACE('}'),
        SEMICOLON(';'),
    }

    enum class Keyword : Token {
        RETURN,
    }

    sealed interface Literal<out T> : Token {
        val value: T

        data class IntLiteral(override val value: Int) : Literal<Int>
    }
}

