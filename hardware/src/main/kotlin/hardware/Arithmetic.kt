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
