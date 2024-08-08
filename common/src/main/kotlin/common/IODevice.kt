package common

interface IODevice {
    val inputPorts: Set<UShort>
    val outputPorts: Set<UShort>

    fun update()
}