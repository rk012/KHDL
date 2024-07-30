package processor

import hardware.DMux
import hardware.Mux
import hardware.Register
import hdl.*

class CpuRegisters(clk: Clock) {
    private val registers = List(8) { Register(clk, 16) }

    private val inputHeader = PinHeader(16)

    private val headerA = PinHeader(3)
    private val headerB = PinHeader(3)

    private val dMux = DMux(3)

    private val muxA = List(16) { Mux(3) }
    private val muxB = List(16) { Mux(3) }

    init {
        (registers zip dMux.out).forEach { (r, w) -> r.w bind w }
        registers.forEach { it.d bind inputHeader.output }

        repeat(8) { i ->
            muxA.map { it.inputs[i] } bind registers[i].out
            muxB.map { it.inputs[i] } bind registers[i].out
        }

        muxA.forEach { it.addr bind headerA.output }
        muxB.forEach { it.addr bind headerB.output }
    }

    val d: InputBus = inputHeader.input
    val w = dMux.input
    val wAddr: InputBus = dMux.addr

    val addrA: InputBus = headerA.input
    val addrB: InputBus = headerB.input

    val a: OutputBus = muxA.map { it.out }
    val b: OutputBus = muxB.map { it.out }
}