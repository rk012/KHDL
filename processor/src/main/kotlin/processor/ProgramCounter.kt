package processor

import hardware.BusSwitch
import hardware.HalfAdder
import hdl.*

class ProgramCounter(clk: Clock, size: Int) {
    init {
        require(size > 1)
    }

    private val adders = List(size) { HalfAdder() }
    private val switch = BusSwitch(size)
    private val dff = List(size) { DFF(clk) }

    init {
        adders.zipWithNext().forEach { (h0, h1) ->
            h0.b bind h1.carry
        }

        switch.a bind adders.map(HalfAdder::out)

        dff.map(DFF::d) bind switch.out

        adders.map(HalfAdder::a) bind dff.map(DFF::out)
    }

    val input: InputBus = switch.b
    val en = adders.last().b
    val w = switch.select

    val out: OutputBus = dff.map(DFF::out)
}