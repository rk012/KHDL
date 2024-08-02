package processor

import hardware.BusSwitch
import hdl.InputBus
import hdl.OutputBus
import hdl.multiInputBus
import hdl.multiInputPins

class PartialWrite(fullSize: Int) {
    init {
        require(fullSize % 2 == 0)
    }

    private val upperSwitch = BusSwitch(fullSize / 2)
    private val lowerSwitch = BusSwitch(fullSize / 2)

    val oldVal: InputBus = upperSwitch.a + lowerSwitch.b
    val partial: InputBus = multiInputBus(upperSwitch.b, lowerSwitch.a)
    val sideFlag = multiInputPins(upperSwitch.select, lowerSwitch.select)

    val newVal: OutputBus = upperSwitch.out + lowerSwitch.out
}