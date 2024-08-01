package common

interface IODevice {
    val inputPorts: Set<Int>
    val outputPorts: Set<Int>

    fun update()
}