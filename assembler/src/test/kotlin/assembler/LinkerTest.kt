package assembler

import VirtualMachine
import common.*
import kotlin.test.Test
import kotlin.test.assertEquals

class LinkerTest {
    private fun resolveIpRelAddr(offset: Int) = arrayOf(  // <- IP - 4
        CpuInstruction.SET(true, WritableRegister.P, 0x00),
        CpuInstruction.SET(false, WritableRegister.P, offset + 4),
        CpuInstruction.MOV(ReadOnlyRegister.IP, WritableRegister.Q),
        CpuInstruction.ALU(false, WritableRegister.Q, WritableRegister.P, AluOperation.A_MINUS_B), // <- IP
        CpuInstruction.MEM(false, WritableRegister.P, WritableRegister.Q),  // P+Q -> dest
        CpuInstruction.ALU(false, WritableRegister.P, WritableRegister.Q, AluOperation.A_PLUS_B),  // P -> dest
    )

    @Test
    fun linkedCalls() {
        val ac = ObjectFile(
            imports = listOf("b"),
            exports = mapOf("a" to 2u, "c" to 0u),

            listOf(
                // c
                CpuInstruction.ALU(false, WritableRegister.A, WritableRegister.B, AluOperation.A_PLUS_B), // <- 0
                CpuInstruction.HLT,

                // a
                *resolveIpRelAddr(2),
                CpuInstruction.CMP(true, JumpCondition(0b111), WritableRegister.P)
            ).map(CpuInstruction::code)
        )

        val b = ObjectFile(
            imports = listOf("const", "c"),
            exports = mapOf("b" to 0u),

            listOf(
                // b
                CpuInstruction.SET(true, WritableRegister.A, 0x01),
                CpuInstruction.SET(false, WritableRegister.A, 0x20),
                *resolveIpRelAddr(3),  // P -> const, len=6
                CpuInstruction.MEM(false, WritableRegister.P, WritableRegister.B),
                *resolveIpRelAddr(9),  // P -> c
                CpuInstruction.CMP(true, JumpCondition(0b111), WritableRegister.P)
            ).map(CpuInstruction::code)
        )

        val const = ObjectFile(
            imports = emptyList(),
            exports = mapOf("const" to 0u),

            listOf(0x065F)
        )

        val linked = link("a", const, ac, b)

        val vm = VirtualMachine(linked.bytecode)
        vm.runUntilHalt()
        assertEquals(0x065F + 0x0120, vm.debugRegister(WritableRegister.P).toInt())
    }
}