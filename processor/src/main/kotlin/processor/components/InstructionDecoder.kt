package processor.components

import hardware.DMux
import hdl.InputBus
import hdl.PinHeader
import hdl.PinSource
import hdl.bind

internal class InstructionDecoder {
    private val header = PinHeader(16)
    private val dmux = DMux(3)

    private val one = PinSource(true)

    init {
        dmux.addr bind header.output.subList(0,3)
        dmux.input bind one
    }

    val instruction: InputBus = header.input

    val hlt = dmux.out[0]
    val nop = dmux.out[1]
    val mov = dmux.out[2]
    val set = dmux.out[3]
    val cmp = dmux.out[4]
    val alu = dmux.out[5]
    val mem = dmux.out[6]
    val  io = dmux.out[7]

    val ab = header.output[3]

    val cond = header.output.subList(4, 7)

    val xReg = header.output.subList(3, 7)
    val regA = header.output.subList(4, 7)
    val regB = header.output.subList(7, 10)

    val aluOp = header.output.subList(10, 16)

    val iVal = header.output.subList(8, 16)
}