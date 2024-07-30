package common

import kotlin.test.Test
import kotlin.test.assertEquals

class InstructionTest {
    @Test
    fun decodeTest() {
        val instructions = listOf(
            Instruction.NOP,
            Instruction.HLT,
            Instruction.MOV(ReadOnlyRegister.IP, WritableRegister.A),
            Instruction.MOV(WritableRegister.B, WritableRegister.C),
            Instruction.SET(true, WritableRegister.B,254),
            Instruction.JMP(JumpCondition(eq=false, lt=true, gt=true), WritableRegister.C),
            Instruction.ALU(false, WritableRegister.D, WritableRegister.Q, AluOperation.OR),
            Instruction.MEM(true, WritableRegister.SP, WritableRegister.P),
            Instruction.IO(true, WritableRegister.A, WritableRegister.B)
        )

        val decoded = instructions.map(Instruction::code).map(Instruction::parse)

        assertEquals(instructions, decoded)
    }
}