package processor.components

import hardware.BusSwitch
import hdl.InputBus
import hdl.OutputBus
import hdl.PinHeader
import hdl.bind

internal class XReg {
    private val addrHeader = PinHeader(4)

    private val s1 = BusSwitch(16)
    private val s2 = BusSwitch(16)

    init {
        addrHeader.output[0] bind s2.select
        addrHeader.output[3] bind s1.select

        s2.b bind s1.out
    }

    val cpuRegOut: InputBus = s2.a
    val ipRegOut: InputBus = s1.a
    val flagsRegOut: InputBus = s1.b

    val xAddr: InputBus = addrHeader.input

    val xRegOut: OutputBus = s2.out
}