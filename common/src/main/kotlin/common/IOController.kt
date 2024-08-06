package common

abstract class IOController {
    private val readPorts = mutableSetOf<Int>()
    private val writePorts = mutableSetOf<Int>()

    private val _devices = mutableListOf<IODevice>()
    protected val devices: List<IODevice> get() = _devices

    fun <T : IODevice> install(newDevice: (IOController) -> T) = newDevice(this).also {
        require((readPorts intersect it.inputPorts).isEmpty())
        require((writePorts intersect it.outputPorts).isEmpty())

        readPorts += it.inputPorts
        writePorts += it.outputPorts

        _devices.add(it)
    }

    abstract fun readOutput(port: Int): Int
    abstract fun writeInput(port: Int, value: Int)

    operator fun get(port: Int): Int = readOutput(port)
    operator fun set(port: Int, value: Int) = writeInput(port, value)
}