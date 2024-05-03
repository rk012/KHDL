package hdl

import kotlin.test.Test

class GateTest {
    @Test
    fun nandTest() {
        val nand = Nand()

        val src = BusSource(2)
        listOf(nand.a, nand.b) bind src.outputBus

        TestBus(listOf(nand.out)).test(
            listOf(
                0b00,
                0b01,
                0b10,
                0b11
            ),
            listOf(
                0b1,
                0b1,
                0b1,
                0b0
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