package common

interface Computer {
    val rom: Bytecode
    val ioController: IOController

    fun runUntilHalt()
    fun runNextInstruction()
    fun reset()

    fun debugRegister(register: Register): Short
    fun debugMemory(address: Int): Short
    fun runInstructions(instructions: List<Instruction>)
}