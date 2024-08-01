package hardware

import hdl.*
import kotlin.random.Random
import kotlin.test.Test

class MemTest {
    @Test
    fun register() {
        val clk = Clock()

        val reg = Register(clk, 1)

        val src = BusSource(2)

        reg.d bind src.outputBus.subList(0, 1)
        reg.w bind src.outputBus[1]

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
        val clk = Clock()
        val ram = Ram(clk, 3, 2)

        val src = BusSource(6)

        ram.input + ram.addr + listOf(ram.w) bind src.outputBus

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

    class VirtualRamTester(clk: Clock) {
        private val ramA = Ram(clk, 2, 2)
        private val ramB = VirtualRam(clk, 2, 2)

        private val xorA = Xor()
        private val xorB = Xor()
        private val or = Or()

        init {
            listOf(xorA.a, xorB.a) bind ramA.out
            listOf(xorA.b, xorB.b) bind ramB.out
            or.a bind xorA.out
            or.b bind xorB.out
        }

        val input = multiInputBus(ramA.input, ramB.input)
        val addr = multiInputBus(ramA.addr, ramB.addr)
        val w = multiInputPins(ramA.w, ramB.w)

        val err = or.out
    }

    @Test
    fun virtualRam() {
        val rand = Random(37)
        val clk = Clock()
        val tester = VirtualRamTester(clk)
        val src = BusSource(5)

        tester.input + tester.addr + listOf(tester.w) bind src.outputBus

        val lines = List(1000) { rand.nextBits(5) shl 1}

        TestBus(listOf(tester.err)).testLines(lines, clk, src)
    }
}