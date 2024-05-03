package hdl

import kotlin.test.Test

class BusTest {
    @Test
    fun busTest() {
        val src = BusSource(4)

        TestBus(src.outputBus).test(
            listOf(
                0b0000,
                0b1111,
                0b0101,
                0b0110,
                0b1001
            ),
            listOf(
                0b0000,
                0b1111,
                0b0101,
                0b0110,
                0b1001
            ),
            Clock(),
            src
        )
    }
}