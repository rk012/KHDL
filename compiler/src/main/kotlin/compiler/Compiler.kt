package compiler

import assembler.*
import assembler.instructions.*
import common.AluOperation
import common.JumpCondition
import common.WritableRegister
import compiler.ast.*
import compiler.ast.Function
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
private fun AsmBuilderScope.evalBiBuiltin(fn: String): Unit = with(requireNotNull(scope).rStack) {
    val name = "__builtin_$fn"

    +mov(r0 to WritableRegister.P)
    +mov(r1 to r0)
    +mov(WritableRegister.P to r1)

    rsave()

    if (name in imports) +callExt(name)
    else +callLocal("__fn_${name}")

    rrestore(2)
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

                // TODO Type analysis for determining correct mul/div/mod function

                is Expression.Binary.Divide -> evalBiBuiltin("idiv")
                is Expression.Binary.Multiply -> evalBiBuiltin("imul")
                is Expression.Binary.Mod -> evalBiBuiltin("imod")

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

        is Expression.FunctionCall -> {
            expr.args.asReversed().forEach {
                eval(it)
            }

            check(rStack.rSize - oldSize == expr.args.size)

            rStack.rsave()

            if (expr.name in imports) +callExt(expr.name)
            else +callLocal("__fn_${expr.name}")

            rStack.rrestore(expr.args.size)
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
fun compileFunction(fn: GlobalElement.FunctionDefinition) = object : CompiledFunction {
    override val name = fn.function.name

    override val assembly = asm {
        newScope()
        scope!!.loadArgs(fn.function.args)
        compileBlock(fn.body)
        exitScope()
    }

}

private fun Expression?.functionCalls(): Sequence<Expression.FunctionCall> = sequence {
    when (this@functionCalls) {
        is Expression.Assignment -> yieldAll(expr.functionCalls())
        is Expression.Unary -> yieldAll(operand.functionCalls())
        is Expression.Binary -> yieldAll(a.functionCalls() + b.functionCalls())
        is Expression.Ternary -> yieldAll(cond.functionCalls() + a.functionCalls() + b.functionCalls())
        is Expression.FunctionCall -> {
            args.forEach { yieldAll(it.functionCalls()) }
            yield(this@functionCalls)
        }
        else -> Unit
    }
}

private fun BlockItem?.functionCalls(): Sequence<Expression.FunctionCall> = sequence {
    when (this@functionCalls) {
        is Declaration -> yieldAll(initializer.functionCalls())
        is Statement.Expr -> yieldAll(expression.functionCalls())
        is Statement.For -> yieldAll(
            init.functionCalls() + cond.functionCalls() + end.functionCalls() + body.functionCalls()
        )
        is Statement.IfElse -> yieldAll(cond.functionCalls() + ifStmt.functionCalls() + elseStmt.functionCalls())
        is Statement.While -> yieldAll(guard.functionCalls() + body.functionCalls())
        is Statement.Return -> yieldAll(expression.functionCalls())
        is Statement.Block -> items.forEach { yieldAll(it.functionCalls()) }
        else -> Unit
    }
}

fun scanTree(root: SourceNode): CompilerContext {
    val declared = mutableMapOf<String, Function>()
    val defined = mutableMapOf<String, Function>()

    root.items.forEach { elem ->
        when (elem) {
            is GlobalElement.ForwardDeclaration -> {
                declared[elem.function.name]?.let {
                    require(it matchesSignature elem.function) { "Mismatched Signature" }
                }

                declared[elem.function.name] = elem.function
            }
            is GlobalElement.FunctionDefinition -> {
                require(elem.function.name !in defined) { "Duplicate function definition" }

                declared[elem.function.name]?.let {
                    require(it matchesSignature elem.function) { "Mismatched Signature" }
                }

                declared[elem.function.name] = elem.function
                defined[elem.function.name] = elem.function

                elem.body.functionCalls().forEach {
                    val fn = requireNotNull(declared[it.name]) { "Undeclared function ${it.name}" }

                    // TODO Type Checking for function call args
                    require(fn.args.size == it.args.size)
                }
            }
        }
    }

    return CompilerContext(declared.keys - defined.keys, defined.keys)
}

data class CompiledSource(
    val asm: Assembly,
    val imports: List<String>,
    val exports: List<String>
)

fun compileSource(sourceCode: String, precompiled: List<CompiledFunction>): CompiledSource {
    val src = """
        |${Builtin.header}
        |
        |$sourceCode
    """.trimMargin()

    val ast = parseTokens(lexer(src))
    val ctx = scanTree(ast)

    precompiled.forEach { ctx.addCompiled(it) }

    with(ctx) {
        ast.items.forEach {
            when (it) {
                is GlobalElement.FunctionDefinition -> ctx.addCompiled(compileFunction(it))
                is GlobalElement.ForwardDeclaration -> Unit
            }
        }
    }

    val preDefs = precompiled.map { it.name }

    return CompiledSource(
        asm = ctx.compiledFunctions(),
        imports = (ctx.imports - preDefs.toSet()).toList(),
        exports = precompiled.map { it.name } + ctx.exports.toList()
    )
}

fun compileSingleSource(sourceCode: String, precompiled: List<CompiledFunction> = emptyList(), offset: Int = 0) = asm {
    +set(WritableRegister.SP, 0)
    +mov(WritableRegister.SP to WritableRegister.BP)
    +callLocal("__fn_main")
    +hlt()

    val src = """
        |$sourceCode
        |
        |${Builtin.src}
    """.trimMargin()

    val compiled = compileSource(src, precompiled)
    require(compiled.imports.isEmpty()) { "Single source cannot use external functions" }

    addAll(compiled.asm)
}.let {
    link(null, assemble(AsmConfig(true, offset), it))
}

fun compileObj(sourceCode: String, precompiled: List<CompiledFunction> = emptyList()): ObjectFile {
    val compiled = compileSource(sourceCode, precompiled)
    val conf = AsmConfig(
        imports = compiled.imports,
        exportLabels = compiled.exports.associateWith { "__fn_$it" }
    )

    return assemble(conf, compiled.asm)
}

fun compileExe(runtime: CRuntime?, vararg objs: ObjectFile): Executable {
    if (runtime == null) return link(null, *objs)

    val includes = objs.toMutableList()
    includes.add(compileObj(Builtin.src))
    includes.add(runtime.compiled())

    return link(runtime.name, *includes.toTypedArray())
}
