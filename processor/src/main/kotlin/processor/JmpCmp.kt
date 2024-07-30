package processor

import hardware.And
import hardware.IsZero
import hardware.Nor
import hardware.Or
import hdl.PinHeader
import hdl.bind

class JmpCmp(wordSize: Int) {
    private val inputHeader = PinHeader(wordSize)
    private val iz = IsZero(wordSize)

    private val nor = Nor()

    private val ea = And()
    private val la = And()
    private val ga = And()

    private val orA = Or()
    private val orB = Or()

    init {
        iz.inputs bind inputHeader.output

        nor.a bind inputHeader.output[0]
        nor.b bind iz.out

        ea.b bind iz.out
        la.b bind inputHeader.output[0]
        ga.b bind nor.out

        orA.a bind la.out
        orA.b bind ga.out
        orB.a bind ea.out
        orB.b bind orA.out
    }

    val input = inputHeader.input

    val eq = ea.a
    val lt = la.a
    val gt = ga.a

    val output = orB.out
}