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

object MultiplyFn : CompiledFunction {
    override val name = "__builtin_mult"

    /*
    int mult(int a, int b) {
        if (b < 0) return -mult(a, -b);

        // L1
        if (a < b) return mult(b, a);

        // L2
        if (b == 0) return 0;

        // L3
        int res = mult(a, b >> 1);

        if (b & 1) res += a

        // L4
        return res
    }
     */

    override val assembly = asm {
        val l1 = Any()
        val l2 = Any()
        val l3 = Any()
        val l4 = Any()

        +getVar(-3, WritableRegister.A)
        +getVar(-2, WritableRegister.B)

        +set(WritableRegister.Q, getLabel(l1))
        +aluP(WritableRegister.A, WritableRegister.B, AluOperation.NEG_B)
        +jle(WritableRegister.Q)

        // -b > 0 <-> b  < 0
        +mov(WritableRegister.P to WritableRegister.B)
        +push(WritableRegister.A)
        +push(WritableRegister.B)
        +callLocal(name)
        +aluP(WritableRegister.A, WritableRegister.A, AluOperation.NEG_A)
        +mov(WritableRegister.P to WritableRegister.A)
        +ret()

        +Label(l1)
        +set(WritableRegister.Q, getLabel(l2))
        +aluP(WritableRegister.A, WritableRegister.B, AluOperation.A_MINUS_B)
        +jge(WritableRegister.Q)

        // a < b
        +push(WritableRegister.B)
        +push(WritableRegister.A)
        +callLocal(name)
        +ret()

        +Label(l2)
        +set(WritableRegister.Q, getLabel(l3))
        +aluP(WritableRegister.A, WritableRegister.B, AluOperation.B)
        +jne(WritableRegister.Q)

        // b == 0
        +set(WritableRegister.A, 0)
        +ret()

        +Label(l3)
        +push(WritableRegister.A)
        +push(WritableRegister.B)
        +push(WritableRegister.A)
        +aluP(WritableRegister.A, WritableRegister.B, AluOperation.B_SHR)
        +mov(WritableRegister.P to WritableRegister.B)
        +push(WritableRegister.B)
        +callLocal(name)
        +pop(WritableRegister.Q)
        +pop(WritableRegister.Q)
        +aluP(WritableRegister.A, WritableRegister.A, AluOperation.A_PLUS_B) // a << 1 = a+a
        +mov(WritableRegister.P to WritableRegister.A)
        +pop(WritableRegister.B)  // old b
        +set(WritableRegister.Q, getLabel(l4))
        +aluP(WritableRegister.B, WritableRegister.B, AluOperation.ONE)
        +aluP(WritableRegister.B, WritableRegister.P, AluOperation.AND)
        +je(WritableRegister.Q)

        // b & 1 != 0
        +pop(WritableRegister.B) // a = res, b = a
        +aluP(WritableRegister.A, WritableRegister.B, AluOperation.A_PLUS_B)
        +mov(WritableRegister.P to WritableRegister.A)


        +Label(l4)
        +ret()
    }
}

class CompilerContext {
    private val loadedFunctions = mutableSetOf<CompiledFunction>()

    fun addCompiled(fn: CompiledFunction) {
        loadedFunctions.add(fn)
    }

    fun AsmBuilderScope.callCompiled(fn: CompiledFunction) {
        addCompiled(fn)
        +callLocal(fn.name)
    }

    fun compiledFunctions(): Assembly = asm {
        loadedFunctions.forEach { fn ->
            +fn.name
            addAll(fn.assembly)
        }
    }
}

context(CompilerContext)
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

        is Expression.Binary -> {
            TODO()
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
                    eval(stmt.expression)
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