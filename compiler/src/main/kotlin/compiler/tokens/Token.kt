package compiler.tokens


sealed interface Token {
    data class Identifier(val value: String) : Token

    sealed interface Symbol : Token {
        val c: Char

        enum class Separator(override val c: Char) : Symbol {
            OPEN_PAREN('('),
            CLOSE_PAREN(')'),
            OPEN_BRACE('{'),
            CLOSE_BRACE('}'),
            SEMICOLON(';'),
        }

        enum class Operator(override val c: Char) : Symbol {
            MINUS('-'),
            TILDE('~'),
            BANG('!'),
        }
    }

    enum class Keyword : Token {
        RETURN,
    }

    sealed interface Literal<out T> : Token {
        val value: T

        data class IntLiteral(override val value: Int) : Literal<Int>
    }
}

