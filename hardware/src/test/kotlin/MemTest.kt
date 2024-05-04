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

        TestBus(reg.out).testLines(
            listOf(
                0b00__0,
                0b00__0,
                0b00__0,
                0b01__0,
                0b00__0,
                0b10__0,
                0b00__0,
                0b11__0,
                0b01__1,
                0b00__0,
                0b01__0,
                0b01__0,
                0b01__0,
                0b10__0,
                0b01__0,
                0b11__0,
                0b10__1,
                0b00__1,
                0b10__1,
                0b01__1,
                0b10__0,
                0b10__0,
                0b10__0,
                0b11__0,
                0b11__1,
                0b00__1,
                0b11__1,
                0b01__1,
                0b11__0,
                0b10__1,
                0b11__1,
                0b11__1,
                0b00__1
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

        TestBus(ram.out).testLines(
            listOf(
                0b10_000_1__00,
                0b10_000_0__10,
                0b10_001_0__00,
                0b11_011_1__00,
                0b01_010_1__00,
                0b00_000_1__10,
                0b11_011_0__11,
                0b11_010_0__01,
                0b11_000_0__00,
                0b00_000_0__00
            ),
            clk,
            src
        )
    }
}