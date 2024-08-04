package hardware

import hdl.*

class Register(clk: Clock, size: Int) {
    init {
        require(size > 0)
    }

    private val dff = List(size) { DFF(clk) }

    private val mux = List(size) { Mux(1) }

    init {
        (dff zip mux).forEach { (dff, mux) ->
            dff.out bind mux.inputs[0]
            mux.out bind dff.d
        }
    }

    val d: InputBus = mux.map { it.inputs[1] }
    val w = multiInputPins(*mux.map { it.addr[0] }.toTypedArray())

    val out: OutputBus = dff.map { it.out }
}

class Ram(clk: Clock, addrSize: Int, wordSize: Int) {
    init {
        require(addrSize > 0 && wordSize > 0)
    }

    private val ramA = if (addrSize == 1) null else Ram(clk, addrSize-1, wordSize)
    private val ramB = if (addrSize == 1) null else Ram(clk, addrSize-1, wordSize)

    private val regA = if (addrSize > 1) null else Register(clk, wordSize)
    private val regB = if (addrSize > 1) null else Register(clk, wordSize)

    private val dMux = DMux(1)
    private val mux = List(wordSize) { Mux(1) }

    init {
        if (addrSize > 1) {
            mux.map { it.inputs[0] } bind ramA!!.out
            mux.map { it.inputs[1] } bind ramB!!.out

            ramA.w bind dMux.out[0]
            ramB.w bind dMux.out[1]
        } else {
            mux.map { it.inputs[0] } bind regA!!.out
            mux.map { it.inputs[1] } bind regB!!.out

            regA.w bind dMux.out[0]
            regB.w bind dMux.out[1]
        }
    }

    val input: InputBus =
        if (addrSize > 1) multiInputBus(ramA!!.input, ramB!!.input)
        else multiInputBus(regA!!.d, regB!!.d)

    val addr: InputBus = listOf(multiInputPins(dMux.addr[0], *mux.map { it.addr[0] }.toTypedArray())) +
            if (addrSize > 1) multiInputBus(ramA!!.addr, ramB!!.addr)
            else emptyList()

    val w: InputPin = dMux.input

    val out: OutputBus = mux.map { it.out }
}

@OptIn(InternalHdlApi::class)
class VirtualRam(clk: Clock, addrSize: Int, wordSize: Int) : ClockedChip {
    private val _input = PinHeader(wordSize)
    private val _addr = PinHeader(addrSize)
    private val _w = PinImpl()

    private val arr = IntArray(1 shl addrSize)

    val input: InputBus = _input.input
    val addr: InputBus = _addr.input
    val w: InputPin = _w

    private var writeCommand: Pair<Int, Int>? = null

    val out: OutputBus = List(wordSize) { i ->
        outputPin { ctx ->
            arr[_addr.output.peekInt(ctx)] and (1 shl i) != 0
        }
    }.reversed()

    init {
        clk.addChip(this)
    }

    override fun tick(nonce: Any) {
        writeCommand = null

        if (_w.peek(nonce)) {
            writeCommand = _addr.output.peekInt(nonce) to _input.output.peekInt(nonce)
        }
    }

    override fun tock() {
        val (addr, value) = writeCommand ?: return
        arr[addr] = value
    }
}
