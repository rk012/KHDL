import hdl.BusSource
import hdl.Clock
import hdl.TestBus
import hdl.bind
import kotlin.test.Test

class MuxTest {
    @Test
    fun mux1() {
        val mux = Mux(1)

        val src = BusSource(3)

        mux.inputs[0] bind src.outputBus[0]
        mux.inputs[1] bind src.outputBus[1]
        mux.addr[0] bind src.outputBus[2]

        TestBus(listOf(mux.out)).test(
            listOf(
                0b000,
                0b001,
                0b010,
                0b011,
                0b100,
                0b101,
                0b110,
                0b111
            ),
            listOf(
                0b0,
                0b0,
                0b0,
                0b1,
                0b1,
                0b0,
                0b1,
                0b1
            ),
            Clock(),
            src
        )
    }

    @Test
    fun mux8() {
        val mux = Mux(3)

        val src = BusSource(11)

        mux.inputs bind src.outputBus.subList(0, 8)
        mux.addr bind src.outputBus.subList(8, 11)

        TestBus(listOf(mux.out)).test(
            listOf(
                0b10101010_1101,
                0b10101010_1010,
                0b10101010_0101,
                0b10101010_0010,
            ),
            listOf(
                0b1,
                0b0,
                0b1,
                0b0
            ),
            Clock(),
            src
        )
    }

    @Test
    fun dMux1() {
        val dMux = DMux(1)

        val src = BusSource(2)

        dMux.input bind src.outputBus[0]
        dMux.addr[0] bind src.outputBus[1]

        TestBus(dMux.out).test(
            listOf(
                0b00,
                0b01,
                0b10,
                0b11
            ),
            listOf(
                0b00,
                0b00,
                0b10,
                0b01
            ),
            Clock(),
            src
        )
    }

    @Test
    fun dMux8() {
        val dMux = DMux(3)

        val src = BusSource(4)

        dMux.input bind src.outputBus[0]
        dMux.addr bind src.outputBus.subList(1, 4)

        TestBus(dMux.out).test(
            listOf(
                0b0000,
                0b0001,
                0b0010,
                0b0011,
                0b0100,
                0b0101,
                0b0110,
                0b0111,
                0b1000,
                0b1001,
                0b1010,
                0b1011,
                0b1100,
                0b1101,
                0b1110,
                0b1111
            ),
            listOf(
                0b00000000,
                0b00000000,
                0b00000000,
                0b00000000,
                0b00000000,
                0b00000000,
                0b00000000,
                0b00000000,
                0b10000000,
                0b01000000,
                0b00100000,
                0b00010000,
                0b00001000,
                0b00000100,
                0b00000010,
                0b00000001
            ),
            Clock(),
            src
        )
    }
}