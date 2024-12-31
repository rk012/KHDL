package compiler.ast

import compiler.tokens.Token

sealed interface BlockItem {
    companion object : Parser<BlockItem> by parseAny(
        Declaration,
        Statement
    )
}

data class Declaration(val type: Type, val name: String, val initializer: Expression?) : BlockItem {
    companion object : Parser<Declaration> by parser ({
        val type = Type.parse()
        val name = match<Token.Identifier>().value
        var initializer: Expression? = null

        if (peek() == Token.Symbol.Operator.ASSIGN) {
            match(Token.Symbol.Operator.ASSIGN)
            initializer = Expression.parse()
        }

        Declaration(type, name, initializer).also { match(Token.Symbol.Separator.SEMICOLON) }
    })
}

sealed interface Statement : BlockItem {
    data class Block(val items: List<BlockItem>) : Statement {
        companion object : Parser<Block> by parser({
            match(Token.Symbol.Separator.OPEN_BRACE)
            val items = mutableListOf<BlockItem>()

            while (true) {
                items.add(parseAnyOrNull(BlockItem).parse() ?: break)
            }

            match(Token.Symbol.Separator.CLOSE_BRACE)

            Block(items)
        })
    }

    data class Expr(val expression: Expression?) : Statement {
        companion object : Parser<Expr> by parseAny (
            parser("Empty Expr") {
                match(Token.Symbol.Separator.SEMICOLON)
                Expr(null)
            },
            parser("Nonempty Expr") {
                Expr(Expression.parse()).also { match(Token.Symbol.Separator.SEMICOLON) }
            }
        )
    }

    data class IfElse(val cond: Expression, val ifStmt: Statement, val elseStmt: Statement?) : Statement {
        companion object : Parser<IfElse> by parser ({
            match(Token.Keyword.IF)
            match(Token.Symbol.Separator.OPEN_PAREN)
            val cond = Expression.parse()
            match(Token.Symbol.Separator.CLOSE_PAREN)
            val ifStmt = Statement.parse()
            var elseStmt: Statement? = null

            if (peek() == Token.Keyword.ELSE) {
                match(Token.Keyword.ELSE)
                elseStmt = Statement.parse()
            }

            IfElse(cond, ifStmt, elseStmt)
        })
    }


    data class Return(val expression: Expression) : Statement {
        companion object : Parser<Return> by parser ({
            match(Token.Keyword.RETURN)

            Return(Expression.parse()).also { match(Token.Symbol.Separator.SEMICOLON) }
        })
    }

    sealed interface For : Statement {
        val init: BlockItem
        val cond: Expression?
        val end: Expr
        val body: Statement

        data class ForExpr(
            override val init: Expr,
            override val cond: Expression?,
            override val end: Expr,
            override val body: Statement
        ) : For {
            companion object : Parser<ForExpr> by parser({
                match(Token.Keyword.FOR)
                match(Token.Symbol.Separator.OPEN_PAREN)
                val init = Expr.parse()
                val cond = Expr.parse()
                val end = if (peek() != Token.Symbol.Separator.CLOSE_PAREN) Expression.parse() else null
                match(Token.Symbol.Separator.CLOSE_PAREN)
                val body = Statement.parse()

                ForExpr(
                    init,
                    cond.expression,
                    Expr(end),
                    body
                )
            })
        }

        data class ForDecl(
            override val init: Declaration,
            override val cond: Expression?,
            override val end: Expr,
            override val body: Statement
        ) : For {
            companion object : Parser<ForDecl> by parser({
                match(Token.Keyword.FOR)
                match(Token.Symbol.Separator.OPEN_PAREN)
                val init = Declaration.parse()
                val cond = Expr.parse()
                val end = if (peek() != Token.Symbol.Separator.CLOSE_PAREN) Expression.parse() else null
                match(Token.Symbol.Separator.CLOSE_PAREN)
                val body = Statement.parse()

                ForDecl(
                    init,
                    cond.expression,
                    Expr(end),
                    body
                )
            })
        }

        companion object : Parser<For> by parseAny(
            ForExpr,
            ForDecl
        )
    }

    data class While(val guard: Expression, val body: Statement, val isDoWhile: Boolean) : Statement {
        companion object : Parser<While> by parseAny(
            parser("While") {
                match(Token.Keyword.WHILE)
                match(Token.Symbol.Separator.OPEN_PAREN)
                val guard = Expression.parse()
                match(Token.Symbol.Separator.CLOSE_PAREN)
                val body = Statement.parse()

                While(guard, body, false)
            },
            parser("Do") {
                match(Token.Keyword.DO)
                val body = Statement.parse()
                match(Token.Keyword.WHILE)
                match(Token.Symbol.Separator.OPEN_PAREN)
                val guard = Expression.parse()
                match(Token.Symbol.Separator.CLOSE_PAREN)
                While(guard, body, true)
            }
        )
    }

    data object Break : Statement, Parser<Break> by parser({
        match(Token.Keyword.BREAK)
        match(Token.Symbol.Separator.SEMICOLON)
        Break
    })

    data object Continue : Statement, Parser<Continue> by parser({
        match(Token.Keyword.CONTINUE)
        match(Token.Symbol.Separator.SEMICOLON)
        Continue
    })


    companion object : Parser<Statement> by parseAny(
        Block,
        Expr,
        IfElse,
        Return,
        For,
        While,
        Break,
        Continue
    )
}
