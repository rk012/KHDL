package processor

import hardware.And
import hardware.Nor
import hardware.Or
import hardware.Xor
import hdl.InputBus
import hdl.multiInputPins

class JmpCmp {
    private val xor = Xor()
    private val nor = Nor()

    private val andE = And()
    private val andL = And()
    private val andG = And()

    private val orLG = Or()
    private val orELG = Or()

    init {
        nor.b bind xor.out

        andL.b bind xor.out
        andG.b bind nor.out

        orLG.a bind andL.out
        orLG.b bind andG.out
        orELG.a bind andE.out
        orELG.b bind orLG.out
    }

    val eq = multiInputPins(andE.b, nor.a)
    val neg = xor.a
    val overflow = xor.b

    val cond: InputBus = listOf(andE.a, andL.a, andG.a)

    val output = orELG.out
}