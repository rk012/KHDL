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

interface ClockedChip {
    fun tick(nonce: Int? = null)
    fun tock(nonce: Int? = null)
}

class DFF : ClockedChip {
    private val _in = PinImpl()

    val d: InputPin = _in

    private var x = false
    private var y = false

    val out = object : OutputPin {
        override val peek = DeepRecursiveFunction<Pair<Int?, Set<Any>>, Boolean> {
            y
        }
    }

    override fun tick(nonce: Int?) {
        x = _in.peek(nonce)
    }

    override fun tock(nonce: Int?) {
        y = x
    }

}

abstract class StatefulChip : ClockedChip {
    private val subchips = mutableListOf<ClockedChip>()

    protected fun <T : ClockedChip> T.register() = this@register.also { subchips.add(it) }

    override fun tick(nonce: Int?) {
        subchips.forEach { it.tick(nonce) }
    }

    override fun tock(nonce: Int?) {
        subchips.forEach { it.tock(nonce) }
    }
}
