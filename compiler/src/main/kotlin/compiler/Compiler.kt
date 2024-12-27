package compiler

import assembler.*
import assembler.instructions.*
import common.AluOperation
import common.JumpCondition
import common.WritableRegister
import compiler.ast.*
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
private fun AsmBuilderScope.evalShift(shl: Boolean): Unit = with(scope.rStack) {
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
fun AsmBuilderScope.eval(expr: Expression): Unit = with(scope) {
    when (expr) {
        is Expression.Literal<*> -> rStack.rpushLit((expr.literal as Token.Literal.IntLiteral).value)

        is Expression.Variable -> rpushVar(expr.name)

        is Expression.Unary -> {
            eval(expr.operand)

            when (expr) {
                is Expression.Unary.LogicalNot -> rStack.uCmp(JumpCondition(eq = true))

                is Expression.Unary.BitwiseNot -> rStack.uOp(AluOperation.NOT_A)
                is Expression.Unary.Negate -> rStack.uOp(AluOperation.NEG_A)
            }
        }

        is Expression.Binary -> {
            val oldSize = rStack.rSize
            eval(expr.a)
            check(rStack.rSize - oldSize == 1)

            val lop = when (expr) {
                is Expression.Binary.LogicalAnd -> true
                is Expression.Binary.LogicalOr -> false
                else -> null
            }

            if (lop != null) {
                val end = Any()
                +set(WritableRegister.Q, getLabel(end))
                +aluP(rStack.r0, rStack.r0, AluOperation.A)

                if (lop) +je(WritableRegister.Q)  // &&
                else +jne(WritableRegister.Q)  // ||

                rStack.rpop()
                eval(expr.b)

                +Label(end)
                return
            }

            eval(expr.b)
            check(rStack.rSize - oldSize == 2)

            when (expr) {
                is Expression.Binary.Add -> rStack.binOp(AluOperation.A_PLUS_B)
                is Expression.Binary.Subtract -> rStack.binOp(AluOperation.A_MINUS_B)

                is Expression.Binary.Divide -> TODO()
                is Expression.Binary.Multiply -> TODO()
                is Expression.Binary.Mod -> TODO()

                is Expression.Binary.BitwiseAnd -> rStack.binOp(AluOperation.AND)
                is Expression.Binary.BitwiseOr -> rStack.binOp(AluOperation.OR)
                is Expression.Binary.BitwiseXor -> rStack.binOp(AluOperation.XOR)
                is Expression.Binary.Cmp -> {
                    rStack.binOp(AluOperation.A_MINUS_B)
                    rStack.uCmp(expr.cond)
                }

                is Expression.Binary.Shl -> evalShift(true)
                is Expression.Binary.Shr -> evalShift(false)

                is Expression.Binary.LogicalAnd -> error("Unreachable")
                is Expression.Binary.LogicalOr -> error("Unreachable")
            }
        }

        is Expression.Ternary -> with(scope.rStack) {
            val initSize = rSize
            val lb = Any()
            val end = Any()

            eval(expr.cond)
            +localRef(lb)
            +aluQ(r0, r0, AluOperation.A)
            +je(WritableRegister.P)

            rpop()
            check(rSize == initSize)
            eval(expr.a)
            +localRef(end)
            +jmp(WritableRegister.P)

            +Label(lb)
            rforget()
            check(rSize == initSize)
            eval(expr.b)

            +Label(end)
            check(rSize == initSize + 1)
        }

        is Expression.Assignment -> {
            eval(expr.expr)
            rStack.rpush()
            +mov(rStack.r1 to rStack.r0)
            rpopVar(expr.name)
        }
    }
}

context(CompilerContext)
fun AsmBuilderScope.compileStatement(stmt: Statement) {
    when (stmt) {
        is Statement.Expr -> {
            eval(stmt.expression)
            scope.rStack.rpop()
        }

        is Statement.Return -> {
            eval(stmt.expression)
            check(scope.rStack.rSize == 1)
            scope.rStack.rpop()
            +ret()
        }

        is Statement.IfElse -> with(scope.rStack) {
            val lb = Any()
            val end = Any()

            eval(stmt.cond)
            +localRef(lb)
            +aluQ(r0, r0, AluOperation.A)
            +je(WritableRegister.P)

            rpop()
            check(rSize == 0)
            compileStatement(stmt.ifStmt)

            if (stmt.elseStmt != null) {
                +localRef(end)
                +jmp(WritableRegister.P)
            }

            +Label(lb)
            if (stmt.elseStmt != null) {
                rfake()
                rpop()
                check(rSize == 0)
                compileStatement(stmt.elseStmt)
            }

            +Label(end)
            check(rSize == 0)
        }
    }
}

context(CompilerContext)
fun compileFunction(fn: FunctionDeclaration) = object : CompiledFunction {
    override val name = "__fn_${fn.function.name}"

    override val assembly = asm {
        fn.body.forEach { stmt ->
            check(scope.rStack.rSize == 0)

            when (stmt) {
                is Statement -> compileStatement(stmt)

                is Declaration -> {
                    scope.newVar(stmt.name, stmt.type.wordSize)
                    if (stmt.initializer != null) {
                        eval(stmt.initializer)
                        scope.rpopVar(stmt.name)
                    }
                }
            }

            check(scope.rStack.rSize == 0)
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