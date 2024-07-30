package hardware

import hdl.BusSource
import hdl.Clock
import hdl.TestBus
import hdl.bind
import kotlin.test.Test

class ArithmeticTest {
    @Test
    fun adder() {
        val adder = FullAdder()

        val src = BusSource(3)

        listOf(adder.a, adder.b, adder.carryIn) bind src.outputBus

        TestBus(listOf(adder.carryOut, adder.out)).testLines(
            listOf(
                0b000__00,
                0b001__01,
                0b010__01,
                0b011__10,
                0b100__01,
                0b101__10,
                0b110__10,
                0b111__11
            ),
            Clock(),
            src
        )
    }

    @Test
    fun isZero() {
        val isZero = IsZero(4)
        val src = BusSource(4)
        isZero.inputs bind src.outputBus

        TestBus(listOf(isZero.out)).testLines(
            listOf(
                0b0000__1,
                0b0001__0,
                0b0010__0,
                0b0011__0,
                0b0100__0,
                0b0101__0,
                0b0110__0,
                0b0111__0,
                0b1000__0,
                0b1001__0,
                0b1010__0,
                0b1011__0,
                0b1100__0,
                0b1101__0,
                0b1110__0,
                0b1111__0
            ),
            Clock(),
            src
        )
    }
}