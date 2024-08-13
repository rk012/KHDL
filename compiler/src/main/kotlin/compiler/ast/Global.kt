package compiler.ast

import compiler.parser.Parser
import compiler.parser.parser

data class FunctionDeclaration(
    val function: Function,
    val body: List<Statement>
) {
    companion object : Parser<FunctionDeclaration> by parser({
        val function = Function.parse()
        val body = statementBlockParser.parse()

        FunctionDeclaration(function, body)
    })
}

data class SourceNode(
    val items: List<FunctionDeclaration>
) {
    companion object : Parser<SourceNode> by parser({
        val items = mutableListOf<FunctionDeclaration>()

        while (peek() != null) {
            items.add(FunctionDeclaration.parse())
        }

        SourceNode(items)
    })
}