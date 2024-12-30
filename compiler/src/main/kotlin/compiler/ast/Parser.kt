package compiler.ast

import compiler.tokens.Token
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

typealias TokenStream = List<Token>

data class ParserContext(
    val tokens: TokenStream,
    val index: Int
)

sealed interface ParserResult<out T> {
    data class Success<T>(val value: T, val numTokens: Int) : ParserResult<T>
    sealed interface Failure: ParserResult<Nothing> {
        data class RootFail(val reason: String) : Failure
        data class BindFail(val parsedTokens: List<Token>, val subFail: Failure) : Failure
        data class MultiFail(val subFails: List<Failure>) : Failure {
            init {
                require(subFails.size >= 2)
            }
        }
    }

    fun <U> fold(onSuccess: (T, Int) -> U, onFailure: (Failure) -> U): U = when (this) {
        is Success -> onSuccess(value, numTokens)
        is Failure -> onFailure(this)
    }
}

fun interface Parser<out T> {
    fun runParser(ctx: ParserContext): ParserResult<T>
    fun parse(tokens: TokenStream): ParserResult<T> = runParser(ParserContext(tokens, 0))
}

object AnyToken : Parser<Token> {
    override fun runParser(ctx: ParserContext) = with(ctx) {
        tokens.getOrNull(index)?.let {
            ParserResult.Success(it, 1)
        } ?: ParserResult.Failure.RootFail("Expected token")
    }
}

fun <T> parseAny(vararg parsers: Parser<T>): Parser<T> {
    require(parsers.size >= 2)

    return Parser { ctx ->
        val fails = parsers.map {
            when (val result = it.runParser(ctx)) {
                is ParserResult.Success -> return@Parser result
                is ParserResult.Failure -> result
            }
        }

        check(fails.size == parsers.size)
        ParserResult.Failure.MultiFail(fails)
    }
}

fun <T> parseAnyOrNull(vararg parsers: Parser<T>) = parseAny(*parsers, parser { null })

@RestrictsSuspension
class ParserScope internal constructor(private val ctx: ParserContext) {
    private var index = ctx.index

    val numParsed get() = index - ctx.index
    private val parsedTokens get() = ctx.tokens.subList(ctx.index, index)

    internal lateinit var acc: ParserResult<*>

    suspend fun <T> ParserResult<T>.bind(): T = suspendCoroutineUninterceptedOrReturn { cont ->
        acc = when (this) {
            is ParserResult.Failure -> {
                check(!this@ParserScope::acc.isInitialized)
                ParserResult.Failure.BindFail(parsedTokens, this)
            }

            is ParserResult.Success<T> -> {
                index += numTokens
                cont.resume(value)
                acc // Modified from cont.resume()
            }
        }

        COROUTINE_SUSPENDED
    }

    suspend fun raise(message: String): Nothing =
        ParserResult.Failure.RootFail(message).bind()

    suspend fun <T> Parser<T>.parse(): T = runParser(ctx.copy(index = index)).bind()

    fun peek() = ctx.tokens.getOrNull(index)
    suspend fun next() = AnyToken.parse()
}

fun <T> parser(block: suspend ParserScope.() -> T) = Parser { ctx ->
    val scope = ParserScope(ctx)

    block.startCoroutine(scope, Continuation(EmptyCoroutineContext) {
        scope.acc = ParserResult.Success(it.getOrThrow(), scope.numParsed)
    })

    @Suppress("UNCHECKED_CAST")
    scope.acc as ParserResult<T>
}

fun matchToken(token: Token) = parser {
    val nxt = next()
    if (nxt != token) raise("Expected $token, got $nxt")
}

inline fun <reified T : Token> matchToken() = parser {
    val next = next()
    if (next !is T) raise("Expected token type ${T::class.simpleName}, got $next")
    next
}

suspend fun ParserScope.match(token: Token) = matchToken(token).parse()
suspend inline fun <reified T : Token> ParserScope.match() = matchToken<T>().parse()

fun ParserResult.Failure.message(): String = when(this) {
    is ParserResult.Failure.RootFail -> reason
    is ParserResult.Failure.BindFail -> "${parsedTokens}\n.${subFail.message().replace("\n", "\n.")}"
    is ParserResult.Failure.MultiFail -> subFails.joinToString("\n") { it.message() }
}

