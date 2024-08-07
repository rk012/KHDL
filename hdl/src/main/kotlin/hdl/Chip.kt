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

class PinHeader(size: Int) {
    init {
        require(size > 0)
    }

    private val pins = List(size) { PinImpl() }

    val input: InputBus = pins
    val output: OutputBus = pins
}


class Nand {
    private val _a = PinImpl()
    private val _b = PinImpl()

    val a: InputPin = _a
    val b: InputPin = _b

    val out = outputPin { ctx ->
        !(_a.peek.callRecursive(ctx) && _b.peek.callRecursive(ctx))
    }
}

@InternalHdlApi
interface ClockedChip {
    @InternalHdlApi
    fun tick(nonce: Any)

    @InternalHdlApi
    fun tock()
}

class DFF(clk: Clock) : ClockedChip {
    private val _in = PinImpl()

    val d: InputPin = _in

    private var x = false
    private var y = false

    val out = outputPin { y }

    init {
        clk.addChip(this)
    }

    override fun tick(nonce: Any) {
        x = _in.peek(nonce)
    }

    override fun tock() {
        y = x
    }
}