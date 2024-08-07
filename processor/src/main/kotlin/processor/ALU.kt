package processor

import hardware.*
import hdl.*

internal class ALU(wordSize: Int) {
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

        private val overflowAdder = FullAdder()
        private val overflowXor = Xor()
        private val overflowAnd = And()

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

            overflowAdder.carryIn bind fullAdders[0].carryOut
            overflowAdder.a bind muxAN[0].out
            overflowAdder.b bind muxBN[0].out

            overflowXor.a bind overflowAdder.out
            overflowXor.b bind fullAdders[0].out
            overflowAnd.b bind overflowXor.out
        }

        val a: InputBus = andAZ.map { it.b }
        val b: InputBus = andBZ.map { it.b }

        val za = multiInputPins(*nza.map(Not::a).toTypedArray())
        val na = multiInputPins(*muxAN.map { it.addr[0] }.toTypedArray())

        val zb = multiInputPins(*nzb.map(Not::a).toTypedArray())
        val nb = multiInputPins(*muxBN.map { it.addr[0] }.toTypedArray())

        val f = multiInputPins(*fMux.map { it.addr[0] }.toTypedArray(), overflowAnd.a)
        val no = multiInputPins(*muxON.map { it.addr[0] }.toTypedArray())

        val out: OutputBus = muxON.map { it.out }

        val overflow = overflowAnd.out
    }

    private val controlBits = ControlBits()

    private val mainUnit = MainUnit(wordSize)

    private val isZero = IsZero(wordSize)

    private val xors = List(wordSize) { Xor() }

    private val shift = BusSwitch(wordSize - 1)

    private val select0 = BusSwitch(wordSize)
    private val nand = Nand()

    private val select1 = BusSwitch(wordSize)

    private val and = And()
    private val or = Or()

    init {
        shift.select bind controlBits.c0

        select0.a bind xors.map(Xor::out)
        select0.b bind (listOf(shift.out[0]) + shift.out)

        nand.a bind controlBits.c0
        nand.b bind controlBits.c1

        select0.select bind nand.out

        select1.a bind mainUnit.out

        select1.b bind select0.out

        controlBits.c0 bind or.a
        controlBits.c1 bind or.b
        or.out bind and.a
        controlBits.c2 bind and.b

        select1.select bind and.out

        isZero.inputs bind select1.out
    }

    val a: InputBus = List(wordSize) { i ->
        if (i+1 == wordSize) multiInputPins(mainUnit.a[i], xors[i].a)
        else multiInputPins(mainUnit.a[i], xors[i].a, shift.a[i])
    }

    val b: InputBus = List(wordSize) { i ->
        if (i+1 == wordSize) multiInputPins(mainUnit.b[i], xors[i].b)
        else multiInputPins(mainUnit.b[i], xors[i].b, shift.b[i])
    }

    val za = multiInputPins(mainUnit.za, controlBits.za)
    val na = multiInputPins(mainUnit.na, controlBits.na)
    val zb = multiInputPins(mainUnit.zb, controlBits.zb)
    val nb = multiInputPins(mainUnit.nb, controlBits.nb)
    val f = multiInputPins(mainUnit.f, controlBits.f)
    val no = multiInputPins(mainUnit.no, controlBits.no)

    val out: OutputBus = select1.out

    val neg = select1.out[0]
    val zero = isZero.out
    val overflow = mainUnit.overflow
}