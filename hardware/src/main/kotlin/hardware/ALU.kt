package hardware

import hdl.*

class HalfAdder {
    private val xor = Xor()
    private val and = And()

    val a = multiInputPins(xor.a, and.a)
    val b = multiInputPins(xor.b, and.b)

    val carry = and.out
    val out = xor.out
}

class FullAdder {
    private val h0 = HalfAdder()
    private val h1 = HalfAdder()

    private val or = Or()

    init {
        h0.out bind h1.a
        h0.carry bind or.a
        h1.carry bind or.b
    }

    val a = h0.a
    val b = h0.b

    val carryIn = h1.b

    val carryOut = or.out

    val out = h1.out
}

class IsZero(wordSize: Int) {
    init {
        require(wordSize > 0)
    }

    private val za = if (wordSize > 1) IsZero(wordSize/2) else null
    private val zb = if (wordSize > 1) IsZero(wordSize - wordSize/2) else null

    private val not = Not()
    private val and = And()

    init {
        if (wordSize > 1) {
            za!!.out bind and.a
            zb!!.out bind and.b
        } else {
            not.out bind and.a
            not.out bind and.b
        }
    }

    val inputs: InputBus = if (wordSize > 1) za!!.inputs + zb!!.inputs else listOf(not.a)

    val out = and.out
}

class ALU(wordSize: Int) {
    init {
        require(wordSize > 1)
    }

    internal class ControlBits {
        private val not0 = Not()
        private val not1 = Not()
        private val not2 = Not()

        private val and00 = And()
        private val and10 = And()
        private val and20 = And()

        private val and01 = And()
        private val and11 = And()
        private val and21 = And()

        private val or = Or()
        private val nor = Nor()

        init {
            and00.a bind not0.out
            and10.a bind not1.out
            and20.a bind not2.out

            and01.b bind and00.out
            and11.b bind and10.out
            and21.b bind and20.out

            and01.a bind nor.out
            and11.a bind nor.out
            and21.a bind nor.out

            nor.b bind or.out
        }

        val za = and00.b
        val na = multiInputPins(not0.a, or.a)

        val zb = and10.b
        val nb = multiInputPins(not1.a, or.b)

        val f = and20.b
        val no = multiInputPins(not2.a, nor.a)

        val c0 = and01.out
        val c1 = and11.out
        val c2 = and21.out
    }

    internal class MainUnit(wordSize: Int) {
        init {
            require(wordSize > 1)
        }

        private val nza = List(wordSize) { Not() }
        private val nzb = List(wordSize) { Not() }

        private val andAZ = List(wordSize) { And() }
        private val muxAN = List(wordSize) { Mux(1) }
        private val notA = List(wordSize) { Not() }

        private val andBZ = List(wordSize) { And() }
        private val muxBN = List(wordSize) { Mux(1) }
        private val notB = List(wordSize) { Not() }

        private val fMux = List(wordSize) { Mux(1) }

        private val notOut = List(wordSize) { Not() }
        private val muxON = List(wordSize) { Mux(1) }

        private val halfAdder = HalfAdder()
        private val fullAdders = List(wordSize - 1) { FullAdder() }

        private val andGates = List(wordSize) { And() }

        init {
            andAZ.map(And::a) bind nza.map(Not::out)
            andBZ.map(And::a) bind nzb.map(Not::out)

            notA.map(Not::a) bind andAZ.map(And::out)
            muxAN.map { it.inputs[0] } bind andAZ.map(And::out)
            muxAN.map { it.inputs[1] } bind notA.map(Not::out)

            notB.map(Not::a) bind andBZ.map(And::out)
            muxBN.map { it.inputs[0] } bind andBZ.map(And::out)
            muxBN.map { it.inputs[1] } bind notB.map(Not::out)

            halfAdder.carry bind fullAdders.last().carryIn
            fullAdders.zipWithNext().forEach { (a, b) -> a.carryIn bind b.carryOut }

            (fullAdders.map(FullAdder::a) + halfAdder.a) bind muxAN.map(Mux::out)
            (fullAdders.map(FullAdder::b) + halfAdder.b) bind muxBN.map(Mux::out)

            andGates.map(And::a) bind muxAN.map(Mux::out)
            andGates.map(And::b) bind muxBN.map(Mux::out)

            fMux.map { it.inputs[0] } bind andGates.map(And::out)
            fMux.map { it.inputs[1] } bind (fullAdders.map(FullAdder::out) + halfAdder.out)

            notOut.map(Not::a) bind fMux.map(Mux::out)
            muxON.map { it.inputs[0] } bind fMux.map(Mux::out)
            muxON.map { it.inputs[1] } bind notOut.map(Not::out)
        }

        val a: InputBus = andAZ.map { it.b }
        val b: InputBus = andBZ.map { it.b }

        val za = multiInputPins(*nza.map(Not::a).toTypedArray())
        val na = multiInputPins(*muxAN.map { it.addr[0] }.toTypedArray())

        val zb = multiInputPins(*nzb.map(Not::a).toTypedArray())
        val nb = multiInputPins(*muxBN.map { it.addr[0] }.toTypedArray())

        val f = multiInputPins(*fMux.map { it.addr[0] }.toTypedArray())
        val no = multiInputPins(*muxON.map { it.addr[0] }.toTypedArray())

        val out: OutputBus = muxON.map { it.out }
    }

    private val controlBits = ControlBits()

    private val mainUnit = MainUnit(wordSize)

    private val isZero = IsZero(wordSize)

    private val xors = List(wordSize) { Xor() }

    private val shiftMux = List(wordSize - 1) { Mux(1) }

    private val selectMux0 = List(wordSize) { Mux(1) }
    private val nand = Nand()

    private val selectMux1 = List(wordSize) { Mux(1) }

    private val and = And()
    private val or = Or()

//    private val zero: OutputPin = PinSource(false)

    init {
        shiftMux.forEach { it.addr[0] bind controlBits.c0 }

        (xors zip selectMux0).forEach { (xor, mux) ->
            xor.out bind mux.inputs[0]
        }

        ((listOf(shiftMux[0].out) + shiftMux.map { it.out }) zip selectMux0).forEach { (out, mux) ->
            out bind mux.inputs[1]
        }

        nand.a bind controlBits.c0
        nand.b bind controlBits.c1

        selectMux0.forEach { it.addr[0] bind nand.out }

        (mainUnit.out zip selectMux1).forEach { (out, mux) ->
            out bind mux.inputs[0]
        }

        (selectMux0 zip selectMux1).forEach { (m0, m1) ->
            m0.out bind m1.inputs[1]
        }

        controlBits.c0 bind or.a
        controlBits.c1 bind or.b
        or.out bind and.a
        controlBits.c2 bind and.b

        selectMux1.forEach { it.addr[0] bind and.out }

        isZero.inputs bind selectMux1.map { it.out }
    }

    val a: InputBus = List(wordSize) { i ->
        if (i+1 == wordSize) multiInputPins(mainUnit.a[i], xors[i].a)
        else multiInputPins(mainUnit.a[i], xors[i].a, shiftMux[i].inputs[0])
    }

    val b: InputBus = List(wordSize) { i ->
        if (i+1 == wordSize) multiInputPins(mainUnit.b[i], xors[i].b)
        else multiInputPins(mainUnit.b[i], xors[i].b, shiftMux[i].inputs[1])
    }

    val za = multiInputPins(mainUnit.za, controlBits.za)
    val na = multiInputPins(mainUnit.na, controlBits.na)
    val zb = multiInputPins(mainUnit.zb, controlBits.zb)
    val nb = multiInputPins(mainUnit.nb, controlBits.nb)
    val f = multiInputPins(mainUnit.f, controlBits.f)
    val no = multiInputPins(mainUnit.no, controlBits.no)

    val out: OutputBus = selectMux1.map { it.out }

    val neg = selectMux1[0].out
    val zero = isZero.out
}
