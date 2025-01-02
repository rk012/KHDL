package compiler.ast

import compiler.tokens.Token

sealed interface GlobalElement {
    data class ForwardDeclaration(
        val function: Function
    ) : GlobalElement {
        companion object : Parser<ForwardDeclaration> by parser({
            val fn = Function.parse()
            match(Token.Symbol.Separator.SEMICOLON)
            ForwardDeclaration(fn)
        })
    }

    data class FunctionDefinition(
        val function: Function,
        val body: Statement.Block
    ) : GlobalElement {
        companion object : Parser<FunctionDefinition> by parser({
            val function = Function.parse()
            val body = Statement.Block.parse()

            FunctionDefinition(function, body)
        })
    }

    companion object : Parser<GlobalElement> by parseAny(
        FunctionDefinition,
        ForwardDeclaration
    )
}

data class SourceNode(
    val items: List<GlobalElement>
) {
    companion object : Parser<SourceNode> by parser({
        val items = mutableListOf<GlobalElement>()

        while (peek() != null) {
            items.add(GlobalElement.parse())
        }

        SourceNode(items)
    })
}