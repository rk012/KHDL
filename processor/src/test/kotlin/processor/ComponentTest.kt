package processor

import hdl.BusSource
import hdl.Clock
import hdl.TestBus
import hdl.bind
import kotlin.test.Test

class ComponentTest {
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

    @Test
    fun fetchState() {
        val clk = Clock()
        val chip = FetchState(clk)
        val src = BusSource(1)
        chip.enable bind src.outputBus[0]

        TestBus(listOf(chip.isFetch)).testLines(
            listOf(
                0b0_0,
                0b1_1,
                0b1_0,
                0b1_1,
                0b0_0,
                0b1_1,
                0b1_0,
                0b0_0,
                0b1_1
            ), clk, src
        )
    }

    @Test
    fun cpuRegisters() {
        val clk = Clock()
        val chip = CpuRegisters(clk)
        val src = BusSource(14)
        (chip.d.subList(0, 4) + chip.wAddr + chip.w + chip.addrA + chip.addrB) bind src.outputBus
        chip.d.subList(4, 16) bind BusSource(12).apply { value = List(12) { false } }.outputBus

        TestBus(chip.a.subList(0, 4) + chip.b.subList(0, 4)).testLines(
            listOf(
                //   d  wa w   a   b     a    b
                0b1010_000_0_000_000__0000_0000,
                0b1010_000_1_000_000__0000_0000,
                0b1111_001_1_000_000__1010_1010,
                0b0101_110_1_001_000__1111_1010,
                0b0000_000_0_000_110__1010_0101,
                0b0001_110_1_000_110__1010_0101,
                0b0001_110_0_000_110__1010_0001,
            ),
            clk, src
        )
    }
}