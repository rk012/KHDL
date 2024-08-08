package common

interface Computer {
    val rom: Bytecode
    val ioController: IOController

    fun runUntilHalt()
    fun runNextInstruction()
    fun reset()

    fun debugRegister(register: Register): Int
    fun debugMemory(address: Int): Int
    fun runInstructions(instructions: List<Instruction>)
}