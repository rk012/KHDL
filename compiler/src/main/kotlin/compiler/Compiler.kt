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
    onFailure = { error(it.message()) }
)

interface CompiledFunction {
    val name: String
    val assembly: Assembly
}

context(CompilerContext)
private fun AsmBuilderScope.evalShift(shl: Boolean): Unit = with(requireNotNull(scope).rStack) {
    val loop = Any()
    val end = Any()

    // a << n, r0 = n, r1 = a
    +localRef(end)
    +aluQ(r0, r0, AluOperation.A)
    +je(WritableRegister.P)
    +Label(loop)
    // n != 0
    // a >>= 1 or a <<= 1 <-> a += a
    +aluP(r1, r1, if (shl) AluOperation.A_PLUS_B else AluOperation.A_SHR)
    +mov(WritableRegister.P to r1)
    // n -= 1
    +localRef(loop)
    +aluQ(r0, r0, AluOperation.A_MINUS_1)
    +mov(WritableRegister.Q to r0)
    +jne(WritableRegister.P)
    +Label(end)
    rpop()
}

context(CompilerContext)
fun AsmBuilderScope.eval(expr: Expression): Unit = with(requireNotNull(scope)) {
    val oldSize = rStack.rSize

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
            eval(expr.a)
            check(rStack.rSize - oldSize == 1)

            val lop = when (expr) {
                is Expression.Binary.LogicalAnd -> true
                is Expression.Binary.LogicalOr -> false
                else -> null
            }

            if (lop != null) {
                val end = Any()
                +localRef(end)
                +aluQ(rStack.r0, rStack.r0, AluOperation.A)

                if (lop) +je(WritableRegister.P)  // &&
                else +jne(WritableRegister.P)  // ||

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

        is Expression.Ternary -> with(rStack) {
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

    check(rStack.rSize - oldSize == 1)
}

context(CompilerContext)
fun AsmBuilderScope.compileStatement(stmt: Statement) {
    requireNotNull(scope)

    when (stmt) {
        is Statement.Block -> {
            compileBlock(stmt)
        }

        is Statement.Expr -> {
            if (stmt.expression != null) {
                eval(stmt.expression)
                scope!!.rStack.rpop()
            }
        }

        is Statement.Return -> {
            eval(stmt.expression)
            check(scope!!.rStack.rSize == 1)
            scope!!.rStack.rpop()
            +ret()
        }

        is Statement.IfElse -> with(scope!!.rStack) {
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

        is Statement.For -> {
            newScope()
            with(scope!!.rStack) {
                val loop = Any()
                val cont = Any()
                val end = Any()

                val cond = stmt.cond

                compileBlockItem(stmt.init)

                +Label(loop)
                if (cond != null) {
                    eval(cond)
                    +localRef(end)
                    +aluQ(r0, r0, AluOperation.A)
                    +je(WritableRegister.P)
                    rpop()
                }

                pushBreakContLabel(end, cont)
                compileStatement(stmt.body)
                popBreakContLabel()

                +Label(cont)
                compileStatement(stmt.end)
                +localRef(loop)
                +jmp(WritableRegister.P)

                +Label(end)
                if (cond != null) {
                    rfake()
                    rpop()
                }
            }
            exitScope()
        }

        is Statement.While -> with(scope!!.rStack) {
            val loop = Any()
            val start = Any()
            val end = Any()

            if (stmt.isDoWhile) {
                +localRef(start)
                +jmp(WritableRegister.P)
            }

            +Label(loop)
            eval(stmt.guard)
            +localRef(end)
            +aluQ(r0, r0, AluOperation.A)
            +je(WritableRegister.P)
            rpop()

            +Label(start)
            pushBreakContLabel(end, loop)
            compileStatement(stmt.body)
            popBreakContLabel()
            +localRef(loop)
            +jmp(WritableRegister.P)

            +Label(end)
            rfake()
            rpop()
        }

        Statement.Break -> jmpBreak()
        Statement.Continue -> jmpCont()
    }
}

context(CompilerContext)
fun AsmBuilderScope.compileBlockItem(item: BlockItem) {
    when (item) {
        is Statement -> compileStatement(item)

        is Declaration -> {
            scope!!.newVar(item.name, item.type.wordSize)
            if (item.initializer != null) {
                eval(item.initializer)
                scope!!.rpopVar(item.name)
            }
        }
    }
}

context(CompilerContext)
fun AsmBuilderScope.compileBlock(block: Statement.Block) {
    newScope()

    block.items.forEach {
        check(scope!!.rStack.rSize == 0)
        compileBlockItem(it)
        check(scope!!.rStack.rSize == 0)
    }

    exitScope()
}

context(CompilerContext)
fun compileFunction(fn: FunctionDeclaration) = object : CompiledFunction {
    override val name = "__fn_${fn.function.name}"

    override val assembly = asm {
        compileBlock(fn.body)
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