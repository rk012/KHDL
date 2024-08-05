import common.*

class HardwareComputerTest : ComputerTest {
    override fun new(rom: List<Int>) = HardwareComputer(rom)
}