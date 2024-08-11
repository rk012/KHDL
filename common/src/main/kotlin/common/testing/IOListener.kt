package common.testing

import common.IOController
import common.IODevice
import s

class IOListener(private val controller: IOController, port: UShort = 0u) : IODevice {
    override val inputPorts = setOf(port)
    override val outputPorts = emptySet<UShort>()

    private val _out = mutableListOf<Short>()
    val output: List<Short> get() = _out

    private var last = 0.s

    override fun update() {
        val n = controller[0u]
        if (n == last) return

        last = n
        _out.add(n)
    }
}