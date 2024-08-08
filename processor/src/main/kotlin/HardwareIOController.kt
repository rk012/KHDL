import common.IOController
import common.IODevice
import hdl.*

@OptIn(InternalHdlApi::class)
internal class HardwareIOController(clk: Clock, wordSize: Int, addrSize: Int) : IOController(), ClockedChip {
    init {
        clk.addChip(this)
    }

    private val readBuf = ShortArray(1 shl addrSize)
    private val writeBuf = ShortArray(1 shl addrSize)
    private val outputBuf = ShortArray(1 shl addrSize)

    private val _addr = PinHeader(addrSize)
    private val _data = PinHeader(wordSize)
    private val _w = PinImpl()

    override fun readOutput(port: UShort): Short = readBuf[port.toInt()]

    override fun writeInput(port: UShort, value: Short) {
        writeBuf[port.toInt()] = value
    }

    private var writeCommand: Pair<Int, Int>? = null

    override fun tick(nonce: Any) {
        devices.forEach(IODevice::update)

        writeCommand = null
        if (_w.peek(nonce)) {
            writeCommand = _addr.output.peekInt(nonce) to _data.output.peekInt(nonce)
        }
    }

    override fun tock() {
        writeBuf.copyInto(outputBuf)

        val (addr, value) = writeCommand ?: return
        readBuf[addr] = value.s
    }

    val addr: InputBus = _addr.input
    val d: InputBus = _data.input
    val w: InputPin = _w

    val out: OutputBus = List(wordSize) { i ->
        outputPin { ctx ->
            outputBuf[_addr.output.peekInt(ctx)].toInt() and (1 shl i) != 0
        }
    }.reversed()
}