import common.IOController
import common.IODevice
import hdl.BusSource
import hdl.Clock
import hdl.TestBus
import hdl.bind
import kotlin.test.Test

class IOControllerTest {
    private fun dev(c: IOController) = object : IODevice {
        override val inputPorts = setOf<UShort>(0u)
        override val outputPorts = setOf<UShort>(0u)

        override fun update() {
            c[1u] = (c[0u].toInt() + 1).s
        }
    }

    @Test
    fun ioController() {
        val clk = Clock()
        val ioController = HardwareIOController(clk, 4, 2)
        ioController.install(::dev)

        val src = BusSource(7)
        (ioController.addr + ioController.d + ioController.w) bind src.outputBus

        TestBus(ioController.out).testLines(
            listOf(
                0b01_0000_0__0000,
                0b00_0000_1__0000,
                0b00_0000_0__0000,
                0b01_1100_0__0001,
                0b00_1100_1__0000,
                0b00_1100_0__0000,
                0b01_1100_1__1101,
                0b01_1110_1__1101,
                0b01_1110_0__1101,
                0b01_1110_0__1101,
                0b01_1110_0__1101,
            ), clk, src
        )
    }
}