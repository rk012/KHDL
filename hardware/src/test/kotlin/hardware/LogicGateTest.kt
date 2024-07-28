package hardware

import hdl.BusSource
import hdl.Clock
import hdl.TestBus
import hdl.bind
import kotlin.test.Test

class LogicGateTest {
    @Test
    fun logicGateTest() {
        val not = Not()
        val or = Or()
        val and = And()
        val nor = Nor()
        val xor = Xor()
        val xnor = XNor()

        val src = BusSource(2)

        not.a bind src.outputBus[0]
        listOf(or.a, or.b) bind src.outputBus
        listOf(and.a, and.b) bind src.outputBus
        listOf(nor.a, nor.b) bind src.outputBus
        listOf(xor.a, xor.b) bind src.outputBus
        listOf(xnor.a, xnor.b) bind src.outputBus

        TestBus(listOf(not.out, or.out, and.out, nor.out, xor.out, xnor.out)).testLines(
            listOf(
                0b00__100101,
                0b01__110010,
                0b10__010010,
                0b11__011001
            ),
            Clock(),
            src
        )

    }
}