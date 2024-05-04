package hdl

import kotlin.test.Test

class BusTest {
    @Test
    fun busTest() {
        val src = BusSource(4)

        TestBus(src.outputBus).testLines(
            listOf(
                0b0000__0000,
                0b1111__1111,
                0b0101__0101,
                0b0110__0110,
                0b1001__1001
            ),
            Clock(),
            src
        )
    }
}