package hdl

fun multiInputPins(vararg pins: InputPin): InputPin {
    val pin = PinImpl()

    pins.forEach { it bind pin }

    return pin
}

fun multiInputBus(vararg busses: InputBus): InputBus {
    require(busses.map { it.size }.toSet().size == 1)

    return List(busses[0].size) { i ->
        multiInputPins(*busses.map { it[i] }.toTypedArray())
    }
}

class Nand {
    private val _a = PinImpl()
    private val _b = PinImpl()

    val a: InputPin = _a
    val b: InputPin = _b

    val out = object : OutputPin {
        override val peek = DeepRecursiveFunction<Pair<Int?, Set<Any>>, Boolean> { (nonce, visited) ->
            !(_a.peek.callRecursive(nonce to visited) && _b.peek.callRecursive(nonce to visited))
        }
    }
}

class DFF(clk: Clock) {
    private val _in = PinImpl()

    val d: InputPin = _in

    private var x = false
    private var y = false

    val out = object : OutputPin {
        override val peek = DeepRecursiveFunction<Pair<Int?, Set<Any>>, Boolean> {
            y
        }
    }

    init {
        clk.addChip(this)
    }

    internal fun tick(nonce: Int?) {
        x = _in.peek(nonce)
    }

    internal fun tock() {
        y = x
    }
}