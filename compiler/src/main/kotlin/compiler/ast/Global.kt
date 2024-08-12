package compiler.ast

data class FunctionDeclaration(
    val function: Function,
    val body: List<Statement>
)

data class SourceNode(
    val items: List<FunctionDeclaration>
)