package common

import VirtualMachine

class VirtualMachineTest : ComputerTest {
    override fun new(rom: List<Int>) = VirtualMachine(rom)
}