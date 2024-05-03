import hdl.*

class Mux(addrSize: Int) {
    init {
        require(addrSize > 0)
    }

    private val muxA = if (addrSize == 1) null else Mux(addrSize - 1)
    private val muxB = if (addrSize == 1) null else Mux(addrSize - 1)

    private val andA = And()
    private val andB = And()
    private val or = Or()
    private val not = Not()

    init {
        if (addrSize > 1) {
            muxA!!.out bind andA.b
            muxB!!.out bind andB.b
        }

        not.out bind andA.a

        andA.out bind or.a
        andB.out bind or.b
    }

    val inputs: List<InputPin> = if (addrSize > 1) muxA!!.inputs + muxB!!.inputs else listOf(andA.b, andB.b)

    val addr: InputBus = listOf(multiInputPins(not.a, andB.a)) +
                if (addrSize > 1) multiInputBus(muxA!!.addr,  muxB!!.addr)
                else emptyList()

    val out = or.out
}

class DMux(addrSize: Int) {
    init {
        require(addrSize > 0)
    }

    private val dMuxA = if (addrSize == 1) null else DMux(addrSize - 1)
    private val dMuxB = if (addrSize == 1) null else DMux(addrSize - 1)

    private val andA = And()
    private val andB = And()

    private val not = Not()

    init {
        if (addrSize > 1) {
            andA.out bind dMuxA!!.input
            andB.out bind dMuxB!!.input
        }

        not.out bind andA.a
    }

    val input = multiInputPins(andA.b, andB.b)

    val addr: InputBus = listOf(multiInputPins(not.a, andB.a)) +
            if (addrSize > 1) multiInputBus(dMuxA!!.addr, dMuxB!!.addr)
            else emptyList()

    val out: List<OutputPin> = if (addrSize > 1) dMuxA!!.out + dMuxB!!.out else listOf(andA.out, andB.out)
}
