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