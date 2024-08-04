import common.IOController
import common.IODevice
import hdl.*

@OptIn(InternalHdlApi::class)
class HardwareIOController(clk: Clock, wordSize: Int, addrSize: Int) : IOController, ClockedChip {
    init {
        clk.addChip(this)
    }

    private val readPorts = mutableSetOf<Int>()
    private val writePorts = mutableSetOf<Int>()

    private val devices = mutableListOf<IODevice>()

    private val readBuf = IntArray(wordSize)
    private val writeBuf = IntArray(wordSize)
    private val outputBuf = IntArray(wordSize)

    private val _addr = PinHeader(addrSize)
    private val _data = PinHeader(wordSize)
    private val _w = PinImpl()

    override fun <T : IODevice> install(newDevice: (IOController) -> T) = newDevice(this).also {
        require((readPorts intersect it.inputPorts).isEmpty())
        require((writePorts intersect it.outputPorts).isEmpty())

        readPorts += it.inputPorts
        writePorts += it.outputPorts

        devices.add(it)
    }

    override fun readOutput(port: Int): Int = readBuf[port]

    override fun writeInput(port: Int, value: Int) {
        writeBuf[port] = value
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
        readBuf[addr] = value
    }

    val addr: InputBus = _addr.input
    val d: InputBus = _data.input
    val w: InputPin = _w

    val out: OutputBus = List(wordSize) { i ->
        outputPin { ctx ->
            outputBuf[_addr.output.peekInt(ctx)] and (1 shl i) != 0
        }
    }.reversed()
}