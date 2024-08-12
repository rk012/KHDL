package compiler.ast

import compiler.tokens.Token

sealed interface Type {
    enum class Primitive : Type {
        INT,
    }
}

sealed interface Expression {
    data class Literal<out T>(val literal: Token.Literal<T>) : Expression
}

sealed interface Statement {
    data class Return(val expression: Expression) : Statement
}

data class Function(
    val returnType: Type,
    val name: String
)
