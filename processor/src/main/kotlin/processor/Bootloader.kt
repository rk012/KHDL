package processor

import hardware.IsZero
import hardware.Nor
import hardware.Xor
import hdl.*


@OptIn(InternalHdlApi::class)
class Bootloader(clk: Clock, rom: List<Int>, size: Int) {
    init {
        require(rom.size < 1 shl size)
    }

    private val counter = ProgramCounter(clk, size)

    private val romDataOut: OutputBus = List(size) { i ->
        outputPin { ctx ->
            val line = rom[counter.out.peekInt(ctx)]
            line and (1 shl i) != 0
        }
    }.reversed()

    private val xors = List(size) { Xor() }
    private val iz = IsZero(size)
    private val nor = Nor()

    init {
        counter.input bind BusSource(size).apply { setN(0) }.outputBus
        counter.en bind nor.out

        xors.map(Xor::a) bind counter.out
        xors.map(Xor::b) bind BusSource(size).apply { setN(rom.size) }.outputBus

        iz.inputs bind xors.map(Xor::out)

        nor.b bind iz.out
    }

    val rst = multiInputPins(nor.a, counter.w)

    val w = nor.out

    val addr: OutputBus = counter.out
    val d: OutputBus = romDataOut
}