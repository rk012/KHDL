package compiler

import assembler.AsmBuilderScope
import assembler.Assembly
import assembler.asm
import assembler.instructions.*
import common.AluOperation
import common.JumpCondition
import common.WritableRegister
import compiler.ast.Expression
import compiler.ast.SourceNode
import compiler.ast.Statement
import compiler.tokens.Token
import compiler.tokens.lexer

fun parseTokens(tokens: List<Token>) = SourceNode.parse(tokens).fold(
    onSuccess = { node, _ -> node },
    onFailure = { error(it) }
)

fun AsmBuilderScope.eval(expr: Expression) {
    when (expr) {
        is Expression.Literal<*> -> +set(WritableRegister.A, (expr.literal as Token.Literal.IntLiteral).value)

        is Expression.Unary -> {
            eval(expr.operand)

            val op = when (expr) {
                is Expression.Unary.LogicalNot -> {
                    +aluP(WritableRegister.A, WritableRegister.A, AluOperation.A)
                    +cmp(JumpCondition(eq = true), WritableRegister.A)

                    return
                }

                is Expression.Unary.BitwiseNot -> AluOperation.NOT_A
                is Expression.Unary.Negate -> AluOperation.NEG_A
            }

            +aluP(WritableRegister.A, WritableRegister.A, op)
            +mov(WritableRegister.P to WritableRegister.A)
        }
    }
}

fun compileSource(sourceCode: String): Assembly {
    val ast = parseTokens(lexer(sourceCode))

    return asm {
        ast.items.forEach { fn ->
            +"__fn_${fn.function.name}"

            fn.body.forEach { stmt ->
                when (stmt) {
                    is Statement.Return -> {
                        eval(stmt.expression)
                        +ret()
                    }
                }
            }
        }
    }
}