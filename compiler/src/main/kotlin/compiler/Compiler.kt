package compiler

import assembler.*
import assembler.instructions.*
import common.AluOperation
import common.JumpCondition
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
            eval(expr.b)
            check(rSize - oldSize == 2)

            when (expr) {
                is Expression.Binary.Add -> binOp(AluOperation.A_PLUS_B)
                is Expression.Binary.Subtract -> binOp(AluOperation.A_MINUS_B)
                is Expression.Binary.Divide -> TODO()
                is Expression.Binary.Multiply -> TODO()
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