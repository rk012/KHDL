package hdl

typealias PinEvalContext = Pair<Any, Set<Any>>

interface OutputPin {
    val peek: DeepRecursiveFunction<PinEvalContext, Boolean>

    fun peek(nonce: Any = Any()) = peek.invoke(nonce to emptySet())

    infix fun bind(pin: InputPin) = pin bind this
}

interface InputPin {
    infix fun bind(pin: OutputPin)
}

@InternalHdlApi
fun outputPin(peek: suspend DeepRecursiveScope<PinEvalContext, Boolean>.(PinEvalContext) -> Boolean) = object : OutputPin {
    override val peek = DeepRecursiveFunction(peek)
}

@InternalHdlApi
class PinImpl: OutputPin, InputPin {
    private var inputPin: OutputPin? = null

    private var lastNonce = Any()
    private var lastEval = true

    override fun bind(pin: OutputPin) {
        if (inputPin != null) error("Input pin already set")

        inputPin = pin
    }

    override val peek = DeepRecursiveFunction<Pair<Any, Set<Any>>, Boolean> { (nonce, visited) ->
        if (nonce === lastNonce) return@DeepRecursiveFunction lastEval

        if (this@PinImpl in visited) error("Pin cycle")

        inputPin!!.peek.callRecursive(nonce to visited.plusElement(this@PinImpl)).also {
            lastEval = it
            lastNonce = nonce
        }
    }
}

class PinSource(var value: Boolean = false) : OutputPin {
    override val peek = DeepRecursiveFunction<Pair<Any, Set<Any>>, Boolean> {
        value
    }
}


