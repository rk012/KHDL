package common

import s
import kotlin.test.Test
import kotlin.test.assertEquals

interface ComputerTest {
    private class IOListener(private val controller: IOController) : IODevice {
        override val inputPorts = setOf<UShort>(0u)
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

    fun new(rom: Bytecode): Computer

    @Test
    fun fibs() {
        val c = new(listOf(
            Instruction.SET(true, WritableRegister.A, 0),
            Instruction.SET(false, WritableRegister.A, 1),

            Instruction.SET(true, WritableRegister.B, 0),
            Instruction.SET(false, WritableRegister.B, 0),

            Instruction.SET(true, WritableRegister.D, 0),
            Instruction.SET(false, WritableRegister.D, 0x07),

            Instruction.MOV(WritableRegister.B, WritableRegister.C),

            Instruction.IO(true, WritableRegister.C, WritableRegister.A),  // 0x07
            Instruction.ALU(false, WritableRegister.A, WritableRegister.B, AluOperation.A_PLUS_B),
            Instruction.MOV(WritableRegister.A, WritableRegister.B),
            Instruction.MOV(WritableRegister.P, WritableRegister.A),
            Instruction.ALU(false, WritableRegister.A, WritableRegister.B, AluOperation.A_MINUS_B),
            Instruction.CMP(true, JumpCondition(eq = true, gt = true), WritableRegister.D),
            Instruction.HLT
        ).map(Instruction::code))

        val expected: List<Short> = sequence {
            var a = 1
            var b = 0

            while (true) {
                yield(a.s)
                a = (a+b).also { b = a }
            }
        }.drop(1).takeWhile { it > 0 }.toList()

        val dev = c.ioController.install(::IOListener)

        c.runUntilHalt()

        assertEquals(expected, dev.output)
    }

    @Test
    fun dbgRst() {
        val c = new(listOf(
            Instruction.SET(true, WritableRegister.A, 0),
            Instruction.SET(false, WritableRegister.A, 0xFF),

            Instruction.SET(true, WritableRegister.B, 0),
            Instruction.SET(false, WritableRegister.B, 20),

            Instruction.HLT,
            Instruction.MEM(true, WritableRegister.A, WritableRegister.B),
            Instruction.MOV(WritableRegister.A, WritableRegister.C),
            Instruction.MEM(true, WritableRegister.A, WritableRegister.C),
            Instruction.NOP
        ).map(Instruction::code))

        c.runUntilHalt()
        assertEquals(0xFF, c.debugRegister(WritableRegister.A))
        assertEquals(20, c.debugRegister(WritableRegister.B))

        c.runNextInstruction()
        assertEquals(20, c.debugMemory(0xFF))

        c.runNextInstruction()
        assertEquals(20, c.debugMemory(0xFF))
        c.runNextInstruction()

        assertEquals(0xFF, c.debugMemory(0xFF))

        c.reset()
        c.runUntilHalt()

        c.runInstructions(listOf(
            Instruction.MOV(WritableRegister.B, WritableRegister.Q),
            Instruction.ALU(true, WritableRegister.B, WritableRegister.Q, AluOperation.A_PLUS_B),
        ))

        assertEquals(40, c.debugRegister(WritableRegister.Q))
        assertEquals(0, c.debugRegister(ReadOnlyRegister.FLAGS))

        c.runInstructions(listOf(Instruction.MOV(WritableRegister.Q, WritableRegister.B)))
        c.runNextInstruction()
        assertEquals(40, c.debugMemory(0xFF))

        c.runNextInstruction()
        c.runNextInstruction()
        assertEquals(0xFF, c.debugMemory(0xFF))

        val registers = WritableRegister.entries + ReadOnlyRegister.entries
        val regVals = registers.associateWith { c.debugRegister(it) }

        c.runInstructions(listOf(Instruction.NOP, Instruction.NOP, Instruction.NOP))
        
        assertEquals(regVals, registers.associateWith { c.debugRegister(it) })
    }

    @Test
    fun signedMemTest() {
        val c = new(listOf(
            Instruction.SET(true, WritableRegister.A, 0x80),
            Instruction.SET(false, WritableRegister.A, 0xFF),
            Instruction.MEM(true, WritableRegister.A, WritableRegister.A),
            Instruction.HLT
        ).map(Instruction::code))

        c.runUntilHalt()

        assertEquals(0x80FF.s, c.debugMemory(0x80FF))
    }
}