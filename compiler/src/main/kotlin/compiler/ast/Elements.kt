package compiler.ast

import compiler.tokens.Token

sealed interface Type {
    enum class Primitive : Type {
        INT,
    }

    companion object : Parser<Type> by parser ({
        val id = match<Token.Identifier>()

        Primitive.entries.find { it.name.lowercase() == id.value } ?: raise("Unknown type: ${id.value}")
    })
}

sealed interface Expression {
    data class Literal<out T>(val literal: Token.Literal<T>) : Expression {
        companion object : Parser<Literal<*>> by parser({
            Literal(match<Token.Literal.IntLiteral>())
        })
    }

    sealed interface Unary : Expression {
        val operand: Expression

        data class Negate(override val operand: Expression) : Unary
        data class LogicalNot(override val operand: Expression) : Unary
        data class BitwiseNot(override val operand: Expression) : Unary
    }

    sealed interface Binary : Expression {
        val a: Expression
        val b: Expression

        data class Add(override val a: Expression, override val b: Expression) : Binary
        data class Subtract(override val a: Expression, override val b: Expression) : Binary
        data class Multiply(override val a: Expression, override val b: Expression) : Binary
        data class Divide(override val a: Expression, override val b: Expression) : Binary
    }

    companion object : Parser<Expression> {
        private val unaryExpr: Parser<Expression> by lazy { parseAny(
            parser {
                blockParser(Token.Symbol.Separator.OPEN_PAREN).parse().parseWith(expr)
            },
            parser {
                when (val token = next()) {
                    Token.Symbol.Operator.MINUS -> Unary.Negate(unaryExpr.parse())
                    Token.Symbol.Operator.BANG -> Unary.LogicalNot(unaryExpr.parse())
                    Token.Symbol.Operator.TILDE -> Unary.BitwiseNot(unaryExpr.parse())
                    else -> raise("Expected unary operator, got: $token")
                }
            },
            Literal
        ) }

        private val multiplicativeExpr: Parser<Expression> = parser {
            var root = unaryExpr.parse()

            while (true) {
                root = parseAny(
                    parser {
                        match(Token.Symbol.Operator.ASTERISK)
                        Binary.Multiply(root, unaryExpr.parse())
                    },
                    parser {
                        match(Token.Symbol.Operator.DIV)
                        Binary.Divide(root, unaryExpr.parse())
                    },
                    parser { null }
                ).parse() ?: break
            }

            root
        }

        private val expr: Parser<Expression> = parser {
            var root = multiplicativeExpr.parse()

            while (true) {
                root = parseAny(
                    parser {
                        match(Token.Symbol.Operator.PLUS)
                        Binary.Add(root, multiplicativeExpr.parse())
                    },
                    parser {
                        match(Token.Symbol.Operator.MINUS)
                        Binary.Subtract(root, multiplicativeExpr.parse())
                    },
                    parser { null }
                ).parse() ?: break
            }

            root
        }

        override fun runParser(tokens: TokenStream, index: Int) = expr.runParser(tokens, index)
    }
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
