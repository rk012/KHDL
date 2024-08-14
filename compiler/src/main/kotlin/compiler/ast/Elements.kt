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
    data class Literal<out T>(val literal: Token.Literal<T>) : Expression {
        companion object : Parser<Literal<*>> by parser({
            Literal(match<Token.Literal.IntLiteral>()).also {
                if (peek() != null) error("Expected end of expression")
            }
        })
    }

    sealed interface Unary : Expression {
        val operand: Expression

        data class Negate(override val operand: Expression) : Unary
        data class LogicalNot(override val operand: Expression) : Unary
        data class BitwiseNot(override val operand: Expression) : Unary

        companion object : Parser<Unary> by parser({
            when (val token = next()) {
                Token.Symbol.Operator.MINUS -> Negate(Expression.parse())
                Token.Symbol.Operator.BANG -> LogicalNot(Expression.parse())
                Token.Symbol.Operator.TILDE -> BitwiseNot(Expression.parse())
                else -> error("Expected unary operator, got: $token")
            }
        })
    }

    companion object : Parser<Expression> by parser({
        parseAny(
            Literal,
            Unary
        ).parse()
    })
}

data class Function(
    val returnType: Type,
    val name: String
) {
    companion object : Parser<Function> by parser({
        val returnType = Type.parse()
        val name = match<Token.Identifier>().value

        blockParser(Token.Symbol.Separator.OPEN_PAREN).parse()

        Function(returnType, name)
    })
}
