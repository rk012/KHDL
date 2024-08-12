package compiler

import assembler.Assembly
import assembler.asm
import assembler.instructions.ret
import assembler.instructions.set
import common.WritableRegister
import compiler.ast.Expression
import compiler.ast.Statement
import compiler.ast.parse
import compiler.tokens.Token
import compiler.tokens.lexer

fun compileSource(sourceCode: String): Assembly {
    val ast = parse(lexer(sourceCode))

    return asm {
        ast.items.forEach { fn ->
            +"__fn_${fn.function.name}"

            fn.body.forEach { stmt ->
                when (stmt) {
                    is Statement.Return -> {
                        val expr = stmt.expression as Expression.Literal<*>
                        +set(WritableRegister.A, (expr.literal as Token.Literal.IntLiteral).value)
                    }
                }

                +ret()
            }
        }
    }
}