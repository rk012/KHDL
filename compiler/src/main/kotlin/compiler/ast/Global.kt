package compiler.ast

data class FunctionDeclaration(
    val function: Function,
    val body: List<BlockItem>
) {
    companion object : Parser<FunctionDeclaration> by parser({
        val function = Function.parse()
        val body = blockParser.parse()

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