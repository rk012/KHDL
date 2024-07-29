package hardware

import hdl.InputBus
import hdl.OutputBus
import hdl.multiInputPins

class BusSwitch(size: Int) {
    init {
        require(size > 0)
    }

    private val mux = List(size) { Mux(1) }

    val a: InputBus = mux.map { it.inputs[0] }
    val b: InputBus = mux.map { it.inputs[1] }

    val select = multiInputPins(*mux.map { it.addr[0] }.toTypedArray())

    val out: OutputBus = mux.map(Mux::out)
}