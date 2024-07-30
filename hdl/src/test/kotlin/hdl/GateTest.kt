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
    fun headerTest() {
        val header = PinHeader(2)

        val src = BusSource(2)
        header.input bind src.outputBus

        TestBus(header.output).testLines(
            listOf(
                0b00__00,
                0b01__01,
                0b10__10,
                0b11__11
            ), Clock(), src
        )
    }

    @Test
    fun dffTest() {
        val clk = Clock()
        val dff = DFF(clk)

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