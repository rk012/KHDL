import common.*

class HardwareComputerTest : ComputerTest {
    override fun new(rom: Bytecode) = HardwareComputer(rom)
}