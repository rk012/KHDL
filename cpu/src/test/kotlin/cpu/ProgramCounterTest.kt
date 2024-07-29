package cpu

import hdl.BusSource
import hdl.Clock
import hdl.TestBus
import hdl.bind
import kotlin.test.Test

class ProgramCounterTest {
    @Test
    fun programCounter() {
        val clk = Clock()
        val counter = ProgramCounter(clk, 4)

        val src = BusSource(6)
        counter.input + counter.w + counter.en bind src.outputBus

        TestBus(counter.out).testLines(
            listOf(
                0b0000_1_0__0000,
                0b1111_0_0__0000,
                0b1111_0_1__0000,
                0b1111_0_1__0001,
                0b1111_0_1__0010,
                0b1111_0_1__0011,
                0b1110_1_1__0100,
                0b1111_0_1__1110,
                0b1001_0_1__1111,
                0b1001_0_1__0000,
            ),
            clk, src
        )
    }
}