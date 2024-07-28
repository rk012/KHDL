package hardware

import hdl.Nand
import hdl.multiInputPins

class Not {
    private val nand = Nand()

    val a = multiInputPins(nand.a, nand.b)
    val out = nand.out
}

class Or {
    private val nand = Nand()
    private val notA = Not()
    private val notB = Not()

    init {
        nand.a bind notA.out
        nand.b bind notB.out
    }

    val a = notA.a
    val b = notB.a
    val out = nand.out
}

class And {
    private val nand = Nand()
    private val not = Not()

    init {
        not.a bind nand.out
    }

    val a = nand.a
    val b = nand.b
    val out = not.out
}

class Nor {
    private val or = Or()
    private val not = Not()

    init {
        not.a bind or.out
    }

    val a = or.a
    val b = or.b
    val out = not.out
}

class Xor {
    private val or = Or()
    private val nand = Nand()
    private val and = And()

    init {
        and.a bind or.out
        and.b bind nand.out
    }

    val a = multiInputPins(or.a, nand.a)
    val b = multiInputPins(or.b, nand.b)
    val out = and.out
}

class XNor {
    private val xor = Xor()
    private val not = Not()

    init {
        not.a bind xor.out
    }

    val a = xor.a
    val b = xor.b
    val out = not.out
}

