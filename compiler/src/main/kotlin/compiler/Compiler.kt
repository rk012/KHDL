package compiler

import assembler.*
import assembler.instructions.*
import common.AluOperation
import common.JumpCondition
import common.WritableRegister
import compiler.ast.Expression
import compiler.ast.FunctionDeclaration
import compiler.ast.SourceNode
import compiler.ast.Statement
import compiler.tokens.Token
import compiler.tokens.lexer

fun parseTokens(tokens: List<Token>) = SourceNode.parse(tokens).fold(
    onSuccess = { node, _ -> node },
    onFailure = { error(it) }
)

interface CompiledFunction {
    val name: String
    val assembly: Assembly
}

context(CompilerContext)
private fun AsmBuilderScope.evalShift(shl: Boolean): Unit = with(registerStack) {
    val loop = Any()
    val end = Any()

    // a << n, r0 = n, r1 = a
    +set(WritableRegister.Q, getLabel(end))
    +aluP(r0, r0, AluOperation.A)
    +je(WritableRegister.Q)
    +Label(loop)
    // n != 0
    // a >>= 1 or a <<= 1 <-> a += a
    +aluP(r1, r1, if (shl) AluOperation.A_PLUS_B else AluOperation.A_SHR)
    +mov(WritableRegister.P to r1)
    // n -= 1
    +set(WritableRegister.Q, getLabel(loop))
    +aluP(r0, r0, AluOperation.A_MINUS_1)
    +mov(WritableRegister.P to r0)
    +jne(WritableRegister.Q)
    +Label(end)
    rpop()
}

context(CompilerContext)
fun AsmBuilderScope.eval(expr: Expression): Unit = with(registerStack) {
    when (expr) {
        is Expression.Literal<*> -> rpushLit((expr.literal as Token.Literal.IntLiteral).value)

        is Expression.Unary -> {
            eval(expr.operand)

            when (expr) {
                is Expression.Unary.LogicalNot -> uCmp(JumpCondition(eq = true))

                is Expression.Unary.BitwiseNot -> uOp(AluOperation.NOT_A)
                is Expression.Unary.Negate -> uOp(AluOperation.NEG_A)
            }
        }

        is Expression.Binary -> {
            val oldSize = rSize
            eval(expr.a)
            check(rSize - oldSize == 1)

            val lop = when (expr) {
                is Expression.Binary.LogicalAnd -> true
                is Expression.Binary.LogicalOr -> false
                else -> null
            }

            if (lop != null) {
                val end = Any()
                +set(WritableRegister.Q, getLabel(end))
                +aluP(r0, r0, AluOperation.A)

                if (lop) +je(WritableRegister.Q)  // &&
                else +jne(WritableRegister.Q)  // ||

                rpop()
                eval(expr.b)

                +Label(end)
                return
            }

            eval(expr.b)
            check(rSize - oldSize == 2)

            when (expr) {
                is Expression.Binary.Add -> binOp(AluOperation.A_PLUS_B)
                is Expression.Binary.Subtract -> binOp(AluOperation.A_MINUS_B)

                is Expression.Binary.Divide -> TODO()
                is Expression.Binary.Multiply -> TODO()

                is Expression.Binary.BitwiseAnd -> binOp(AluOperation.AND)
                is Expression.Binary.BitwiseOr -> binOp(AluOperation.OR)
                is Expression.Binary.BitwiseXor -> binOp(AluOperation.XOR)
                is Expression.Binary.Cmp -> {
                    binOp(AluOperation.A_MINUS_B)
                    uCmp(expr.cond)
                }

                is Expression.Binary.Shl -> evalShift(true)
                is Expression.Binary.Shr -> evalShift(false)

                is Expression.Binary.LogicalAnd -> error("Unreachable")
                is Expression.Binary.LogicalOr -> error("Unreachable")
            }
        }
    }
}

context(CompilerContext)
fun compileFunction(fn: FunctionDeclaration) = object : CompiledFunction {
    override val name = "__fn_${fn.function.name}"

    override val assembly = asm {
        fn.body.forEach { stmt ->
            when (stmt) {
                is Statement.Return -> {
                    require(registerStack.rSize == 0)
                    eval(stmt.expression)
                    require(registerStack.rSize == 1)
                    with(registerStack) { rpop() }
                    +ret()
                }
            }
        }
    }

}

fun compileSource(sourceCode: String): Assembly {
    val ast = parseTokens(lexer(sourceCode))
    val ctx = CompilerContext()

    with(ctx) {
        ast.items.forEach {
            ctx.addCompiled(compileFunction(it))
        }
    }

    return ctx.compiledFunctions()
}