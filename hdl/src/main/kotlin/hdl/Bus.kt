package hdl

typealias OutputBus = List<OutputPin>
typealias InputBus = List<InputPin>

fun OutputBus.peekInt(nonce: Int? = null) =
    map { it.peek(nonce) }.fold(0) { acc, bit -> (acc shl 1) or if (bit) 1 else 0 }

infix fun InputBus.bind(bus: OutputBus) {
    require(size == bus.size)

    forEachIndexed { i, pin -> bus[i] bind pin }
}

class BusSource(private val size: Int) {
    private val _bus = List(size) { PinSource() }

    val outputBus: OutputBus = _bus

    var value: List<Boolean> = _bus.map { it.value }
        set(value) {
            field = value

            _bus.forEachIndexed { i, pin -> pin.value = field[i] }
        }

    fun setN(n: Int) {
        value = (0..<size).reversed().map { (1 and (n shr it)) != 0 }
    }
}


