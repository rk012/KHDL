package cpu

import hardware.And
import hardware.Not
import hdl.Clock
import hdl.DFF

class FetchState(clk: Clock) {
    private val dff = DFF(clk)
    private val not = Not()
    private val and = And()

    init {
        dff.d bind and.out
        and.a bind not.out
        not.a bind dff.out
    }

    val enable = and.b
    val isFetch = and.out
}