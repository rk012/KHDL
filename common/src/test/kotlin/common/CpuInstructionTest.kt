package common

import kotlin.test.Test
import kotlin.test.assertEquals

internal class CpuInstructionTest {
    @Test
    fun decodeTest() {
        val instructions = listOf(
            CpuInstruction.NOP,
            CpuInstruction.HLT,
            CpuInstruction.MOV(ReadOnlyRegister.IP, WritableRegister.A),
            CpuInstruction.MOV(WritableRegister.B, WritableRegister.C),
            CpuInstruction.SET(true, WritableRegister.B,254),
            CpuInstruction.CMP(false, JumpCondition(eq=false, lt=true, gt=true), WritableRegister.C),
            CpuInstruction.CMP(true, JumpCondition(eq=false, lt=true, gt=true), WritableRegister.C),
            CpuInstruction.ALU(false, WritableRegister.D, WritableRegister.Q, AluOperation.OR),
            CpuInstruction.MEM(true, WritableRegister.SP, WritableRegister.P),
            CpuInstruction.IO(true, WritableRegister.A, WritableRegister.B)
        )

        val decoded = instructions.map(CpuInstruction::code).map { CpuInstruction.parse(it.toInt()) }

        assertEquals(instructions, decoded)
    }
}