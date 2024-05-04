import hdl.BusSource
import hdl.Clock
import hdl.TestBus
import hdl.bind
import kotlin.test.Test

class MemTest {
    @Test
    fun register() {
        val reg = Register(1)

        val src = BusSource(2)

        reg.d bind src.outputBus.subList(0, 1)
        reg.w bind src.outputBus[1]

        val clk = Clock()
        clk.addChip(reg)

        TestBus(reg.out).test(
            listOf(
                0b00,
                0b00,
                0b00,
                0b01,
                0b00,
                0b10,
                0b00,
                0b11,
                0b01,
                0b00,
                0b01,
                0b01,
                0b01,
                0b10,
                0b01,
                0b11,
                0b10,
                0b00,
                0b10,
                0b01,
                0b10,
                0b10,
                0b10,
                0b11,
                0b11,
                0b00,
                0b11,
                0b01,
                0b11,
                0b10,
                0b11,
                0b11,
                0b00
            ),
            listOf(
                0b0,
                0b0,
                0b0,
                0b0,
                0b0,
                0b0,
                0b0,
                0b0,
                0b1,
                0b0,
                0b0,
                0b0,
                0b0,
                0b0,
                0b0,
                0b0,
                0b1,
                0b1,
                0b1,
                0b1,
                0b0,
                0b0,
                0b0,
                0b0,
                0b1,
                0b1,
                0b1,
                0b1,
                0b0,
                0b1,
                0b1,
                0b1,
                0b1
            ),
            clk,
            src
        )
    }

    @Test
    fun ram() {
        val ram = Ram(3, 2)

        val src = BusSource(6)

        ram.input + ram.addr + listOf(ram.w) bind src.outputBus

        val clk = Clock()
        clk.addChip(ram)

        TestBus(ram.out).test(
            listOf(
                0b10_000_1,
                0b10_000_0,
                0b10_001_0,
                0b11_011_1,
                0b01_010_1,
                0b00_000_1,
                0b11_011_0,
                0b11_010_0,
                0b11_000_0,
                0b00_000_0
            ),
            listOf(
                0b00,
                0b10,
                0b00,
                0b00,
                0b00,
                0b10,
                0b11,
                0b01,
                0b00,
                0b00
            ),
            clk,
            src
        )
    }
}