package compiler.ast

import compiler.tokens.Token

sealed interface Type {
    enum class Primitive : Type {
        INT,
    }

    companion object : Parser<Type> by parser ({
        val id = match<Token.Identifier>()

        Primitive.entries.find { it.name.lowercase() == id.value } ?: error("Unknown type: ${id.value}")
    })
}

sealed interface Expression {
    data class Literal<out T>(val literal: Token.Literal<T>) : Expression

    companion object : Parser<Expression> by parser({
        Literal(match<Token.Literal.IntLiteral>()).also {
            if (peek() != null) error("Expected end of expression")
        }
    })
}

data class Function(
    val returnType: Type,
    val name: String
) {
    companion object : Parser<Function> by parser({
        val returnType = Type.parse()
        val name = match<Token.Identifier>().value

        blockParser(Token.Symbol.OPEN_PAREN).parse()

        Function(returnType, name)
    })
}
