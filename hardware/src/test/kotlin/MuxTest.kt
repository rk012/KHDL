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

        TestBus(listOf(mux.out)).testLines(
            listOf(
                0b000__0,
                0b001__0,
                0b010__0,
                0b011__1,
                0b100__1,
                0b101__0,
                0b110__1,
                0b111__1
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

        TestBus(listOf(mux.out)).testLines(
            listOf(
                0b10101010_1101__1,
                0b10101010_1010__0,
                0b10101010_0101__1,
                0b10101010_0010__0,
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

        TestBus(dMux.out).testLines(
            listOf(
                0b00__00,
                0b01__00,
                0b10__10,
                0b11__01
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

        TestBus(dMux.out).testLines(
            listOf(
                0b0000__00000000,
                0b0001__00000000,
                0b0010__00000000,
                0b0011__00000000,
                0b0100__00000000,
                0b0101__00000000,
                0b0110__00000000,
                0b0111__00000000,
                0b1000__10000000,
                0b1001__01000000,
                0b1010__00100000,
                0b1011__00010000,
                0b1100__00001000,
                0b1101__00000100,
                0b1110__00000010,
                0b1111__00000001
            ),
            Clock(),
            src
        )
    }
}