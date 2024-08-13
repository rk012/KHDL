package compiler.parser

import compiler.ast.SourceNode
import compiler.tokens.Token
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

typealias TokenStream = List<Token>

sealed interface ParserResult<out T> {
    data class Success<T>(val value: T, val numTokens: Int) : ParserResult<T>
    data class Failure(val message: String) : ParserResult<Nothing>

    fun <U> flatMap(block: (T, Int) -> ParserResult<U>): ParserResult<U> = when (this) {
        is Success -> block(value, numTokens)
        is Failure -> this
    }

    fun <U> fold(onSuccess: (T, Int) -> U, onFailure: (String) -> U): U = when (this) {
        is Success -> onSuccess(value, numTokens)
        is Failure -> onFailure(message)
    }
}

fun interface Parser<T> {
    fun runParser(tokens: TokenStream, index: Int): ParserResult<T>
    fun parse(tokens: TokenStream): ParserResult<T> = runParser(tokens, 0)
}

object EmptyParser : Parser<Unit> {
    override fun runParser(tokens: TokenStream, index: Int) = ParserResult.Success(Unit, 0)
}

object AnyToken : Parser<Token> {
    override fun runParser(tokens: TokenStream, index: Int) = tokens.getOrNull(index)?.let {
        ParserResult.Success(it, 1)
    } ?: ParserResult.Failure("Expected token")
}

fun <T> parseAny(vararg parsers: Parser<T>) = Parser { tokens, index ->
    parsers.forEach {
        val result = it.runParser(tokens, index)
        if (result is ParserResult.Success) return@Parser result
    }

    ParserResult.Failure("No parser matched")
}

@RestrictsSuspension
class ParserScope internal constructor(private val tokens: TokenStream, private val startIndex: Int) {
    private var index = startIndex

    val numTokens get() = index - startIndex

    internal lateinit var acc: ParserResult<*>

    suspend fun <T> ParserResult<T>.bind(): T = suspendCoroutineUninterceptedOrReturn { cont ->
        acc = flatMap { value, numTokens ->
            index += numTokens
            cont.resume(value)
            acc
        }

        COROUTINE_SUSPENDED
    }

    suspend fun raise(message: String): Nothing = ParserResult.Failure(message).bind()

    suspend fun <T> Parser<T>.parse(): T = runParser(tokens, index).bind()
    suspend fun <T> TokenStream.parseWith(parser: Parser<T>): T = parser.parse(this@parseWith).bind()

    fun peek() = tokens.getOrNull(index)
    suspend fun next() = AnyToken.parse()
}

fun <T> parser(block: suspend ParserScope.() -> T) = Parser { tokens, index ->
    val scope = ParserScope(tokens, index)

    block.startCoroutine(scope, Continuation(EmptyCoroutineContext) {
        scope.acc = ParserResult.Success(it.getOrThrow(), scope.numTokens)
    })

    @Suppress("UNCHECKED_CAST")
    scope.acc as ParserResult<T>
}

fun matchToken(token: Token) = parser {
    if (next() != token) raise("Expected $token")

    Unit
}

inline fun <reified T : Token> matchToken() = parser {
    val next = next()
    if (next !is T) raise("Expected ${T::class.simpleName}, got $next")
    next
}

suspend fun ParserScope.match(token: Token) = matchToken(token).parse()
suspend inline fun <reified T : Token> ParserScope.match() = matchToken<T>().parse()


fun parseTokens(tokens: List<Token>) = SourceNode.parse(tokens).fold(
    onSuccess = { node, _ -> node },
    onFailure = { error(it) }
)