package compiler.ast

import common.JumpCondition
import compiler.tokens.Token

sealed interface Type {
    val wordSize: Int

    enum class Primitive(override val wordSize: Int) : Type {
        INT(1),
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

    data class Variable(val name: String) : Expression {
        companion object : Parser<Variable> by parser({
            Variable(match<Token.Identifier>().value)
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

        data class Shl(override val a: Expression, override val b: Expression) : Binary
        data class Shr(override val a: Expression, override val b: Expression) : Binary

        data class Cmp(override val a: Expression, override val b: Expression, val cond: JumpCondition) : Binary

        data class LogicalAnd(override val a: Expression, override val b: Expression) : Binary
        data class LogicalOr(override val a: Expression, override val b: Expression) : Binary

        data class BitwiseAnd(override val a: Expression, override val b: Expression) : Binary
        data class BitwiseOr(override val a: Expression, override val b: Expression) : Binary
        data class BitwiseXor(override val a: Expression, override val b: Expression) : Binary
    }

    data class Assignment(val name: String, val expr: Expression) : Expression

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
            Literal,
            Variable
        ) }

        private val multiplicativeExpr: Parser<Expression> = parser {
            var root = unaryExpr.parse()

            while (true) {
                root = parseAnyOrNull(
                    parser {
                        match(Token.Symbol.Operator.ASTERISK)
                        Binary.Multiply(root, unaryExpr.parse())
                    },
                    parser {
                        match(Token.Symbol.Operator.DIV)
                        Binary.Divide(root, unaryExpr.parse())
                    }
                ).parse() ?: break
            }

            root
        }

        private val additiveExpr: Parser<Expression> = parser {
            var root = multiplicativeExpr.parse()

            while (true) {
                root = parseAnyOrNull(
                    parser {
                        match(Token.Symbol.Operator.PLUS)
                        Binary.Add(root, multiplicativeExpr.parse())
                    },
                    parser {
                        match(Token.Symbol.Operator.MINUS)
                        Binary.Subtract(root, multiplicativeExpr.parse())
                    }
                ).parse() ?: break
            }

            root
        }

        private val shiftExpr: Parser<Expression> = parser {
            var root = additiveExpr.parse()

            while (true) {
                root = parseAnyOrNull(
                    parser {
                        match(Token.Symbol.Operator.SHL)
                        Binary.Shl(root, additiveExpr.parse())
                    },
                    parser {
                        match(Token.Symbol.Operator.SHR)
                        Binary.Shr(root, additiveExpr.parse())
                    }
                ).parse() ?: break
            }

            root
        }

        private val relExpr: Parser<Expression> = parser {
            var root = shiftExpr.parse()

            while (true) {
                root = parseAnyOrNull(
                    parser {
                        match(Token.Symbol.Operator.LT)
                        Binary.Cmp(root, shiftExpr.parse(), JumpCondition(lt = true))
                    },
                    parser {
                        match(Token.Symbol.Operator.GT)
                        Binary.Cmp(root, shiftExpr.parse(), JumpCondition(gt = true))
                    },
                    parser {
                        match(Token.Symbol.Operator.LE)
                        Binary.Cmp(root, shiftExpr.parse(), JumpCondition(lt = true, eq = true))
                    },
                    parser {
                        match(Token.Symbol.Operator.GE)
                        Binary.Cmp(root, shiftExpr.parse(), JumpCondition(eq = true, gt = true))
                    }
                ).parse() ?: break
            }

            root
        }

        private val eqExpr: Parser<Expression> = parser {
            var root = relExpr.parse()

            while (true) {
                root = parseAnyOrNull(
                    parser {
                        match(Token.Symbol.Operator.EQ)
                        Binary.Cmp(root, relExpr.parse(), JumpCondition(eq = true))
                    },
                    parser {
                        match(Token.Symbol.Operator.NEQ)
                        Binary.Cmp(root, relExpr.parse(), JumpCondition(lt = true, gt = true))
                    }
                ).parse() ?: break
            }

            root
        }

        private val andExpr: Parser<Expression> = parser {
            var root = eqExpr.parse()

            while (true) {
                root = parseAnyOrNull(
                    parser {
                        match(Token.Symbol.Operator.B_AND)
                        Binary.BitwiseAnd(root, eqExpr.parse())
                    }
                ).parse() ?: break
            }

            root
        }

        private val xorExpr: Parser<Expression> = parser {
            var root = andExpr.parse()

            while (true) {
                root = parseAnyOrNull(
                    parser {
                        match(Token.Symbol.Operator.XOR)
                        Binary.BitwiseXor(root, andExpr.parse())
                    }
                ).parse() ?: break
            }

            root
        }

        private val orExpr: Parser<Expression> = parser {
            var root = xorExpr.parse()

            while (true) {
                root = parseAnyOrNull(
                    parser {
                        match(Token.Symbol.Operator.B_OR)
                        Binary.BitwiseOr(root, xorExpr.parse())
                    }
                ).parse() ?: break
            }

            root
        }

        private val lAndExpr: Parser<Expression> = parser {
            var root = orExpr.parse()

            while (true) {
                root = parseAnyOrNull(
                    parser {
                        match(Token.Symbol.Operator.L_AND)
                        Binary.LogicalAnd(root, orExpr.parse())
                    }
                ).parse() ?: break
            }

            root
        }

        private val lOrExpr: Parser<Expression> = parser {
            var root = lAndExpr.parse()

            while (true) {
                root = parseAnyOrNull(
                    parser {
                        match(Token.Symbol.Operator.L_OR)
                        Binary.LogicalOr(root, lAndExpr.parse())
                    }
                ).parse() ?: break
            }

            root
        }

        private val assignExpr: Parser<Expression> = parseAny(
            parser {
                val name = match<Token.Identifier>().value
                match(Token.Symbol.Operator.ASSIGN)
                val expr = Expression.parse()

                Assignment(name, expr)
            },
            lOrExpr
        )
        
        private val expr = assignExpr

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
