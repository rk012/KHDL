package common

abstract class IOController {
    private val readPorts = mutableSetOf<UShort>()
    private val writePorts = mutableSetOf<UShort>()

    private val _devices = mutableListOf<IODevice>()
    protected val devices: List<IODevice> get() = _devices

    fun <T : IODevice> install(newDevice: (IOController) -> T) = newDevice(this).also {
        require((readPorts intersect it.inputPorts).isEmpty())
        require((writePorts intersect it.outputPorts).isEmpty())

        readPorts += it.inputPorts
        writePorts += it.outputPorts

        _devices.add(it)
    }

    abstract fun readOutput(port: UShort): Short
    abstract fun writeInput(port: UShort, value: Short)

    operator fun get(port: UShort): Short = readOutput(port)
    operator fun set(port: UShort, value: Short) = writeInput(port, value)
}