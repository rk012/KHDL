package common

import kotlin.test.Test
import kotlin.test.assertEquals

interface ComputerTest {
    fun new(rom: List<Int>): Computer

    @Test
    fun fibs() {
        val c: Computer = new(listOf(
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

        val expected: List<Int> = sequence {
            var a = 1
            var b = 0

            while (true) {
                yield(a)
                a = (a+b).also { b = a }
            }
        }.drop(1).takeWhile { it < (1 shl 15) - 1 }.toList()

        val output = mutableListOf<Int>()

        fun dev(controller: IOController) = object : IODevice {
            override val inputPorts = setOf(0)
            override val outputPorts = emptySet<Int>()

            var last = 0

            override fun update() {
                val n = controller[0]
                if (n == last) return

                last = n
                output.add(n)
            }
        }

        c.ioController.install(::dev)

        c.runUntilHalt()

        assertEquals(expected, output)
    }
}