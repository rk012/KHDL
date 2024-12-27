package compiler.tokens


sealed interface Token {
    data class Identifier(val value: String) : Token

    sealed interface Symbol : Token {
        val s: String

        enum class Separator(override val s: String) : Symbol {
            OPEN_PAREN("("),
            CLOSE_PAREN(")"),
            OPEN_BRACE("{"),
            CLOSE_BRACE("}"),
            SEMICOLON(";"),
        }

        enum class Operator(override val s: String) : Symbol {
            SHL("<<"),
            SHR(">>"),

            EQ("=="),
            NEQ("!="),
            LE("<="),
            GE(">="),

            L_AND("&&"),
            L_OR("||"),

            LT("<"),
            GT(">"),

            B_AND("&"),
            B_OR("|"),
            XOR("^"),

            MINUS("-"),
            TILDE("~"),
            BANG("!"),

            PLUS("+"),
            ASTERISK("*"),
            DIV("/"),
            MOD("%"),

            ASSIGN("="),

            QUESTION("?"),
            COLON(":")
        }
    }

    enum class Keyword : Token {
        RETURN,
        IF,
        ELSE
    }

    sealed interface Literal<out T> : Token {
        val value: T

        data class IntLiteral(override val value: Int) : Literal<Int>
    }
}

