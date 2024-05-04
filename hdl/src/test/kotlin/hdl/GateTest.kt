package hdl

import kotlin.test.Test

class GateTest {
    @Test
    fun nandTest() {
        val nand = Nand()

        val src = BusSource(2)
        listOf(nand.a, nand.b) bind src.outputBus

        TestBus(listOf(nand.out)).testLines(
            listOf(
                0b00__1,
                0b01__1,
                0b10__1,
                0b11__0
            ),
            Clock(),
            src
        )
    }

    @Test
    fun dffTest() {
        val clk = Clock()
        val dff = DFF()

        clk.addChip(dff)

        val src = PinSource()
        dff.d bind src

        TestPin(dff.out).test(
            listOf(
                false,
                true,
                false,
                false,
                true,
                false
            ),
            listOf(
                null,
                false,
                true,
                false,
                false,
                true,
            ),
            clk,
            src
        )
    }
}