package common

interface IOController {
    fun <T : IODevice> install(newDevice: (IOController) -> T): T

    fun readOutput(port: Int): Int
    fun writeInput(port: Int, value: Int)

    operator fun get(port: Int): Int = readOutput(port)
    operator fun set(port: Int, value: Int) = writeInput(port, value)
}