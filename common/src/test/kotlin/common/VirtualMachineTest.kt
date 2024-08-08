package common

import VirtualMachine

class VirtualMachineTest : ComputerTest {
    override fun new(rom: Bytecode) = VirtualMachine(rom)
}