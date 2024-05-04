import hdl.*

class Register(size: Int) : StatefulChip() {
    init {
        require(size > 0)
    }

    private val dff = List(size) { DFF().register() }

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

class Ram(addrSize: Int, wordSize: Int) : StatefulChip() {
    init {
        require(addrSize > 0 && wordSize > 0)
    }

    private val ramA = if (addrSize == 1) null else Ram(addrSize-1, wordSize).register()
    private val ramB = if (addrSize == 1) null else Ram(addrSize-1, wordSize).register()

    private val regA = if (addrSize > 1) null else Register(wordSize).register()
    private val regB = if (addrSize > 1) null else Register(wordSize).register()

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
