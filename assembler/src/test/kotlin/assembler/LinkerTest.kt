package assembler

import VirtualMachine
import common.*
import kotlin.test.Test
import kotlin.test.assertEquals

class LinkerTest {
    private fun resolveIpRelAddr(offset: Int) = arrayOf(  // <- IP - 4
        Instruction.SET(true, WritableRegister.P, 0x00),
        Instruction.SET(false, WritableRegister.P, offset + 4),
        Instruction.MOV(ReadOnlyRegister.IP, WritableRegister.Q),
        Instruction.ALU(false, WritableRegister.Q, WritableRegister.P, AluOperation.A_MINUS_B), // <- IP
        Instruction.MEM(false, WritableRegister.P, WritableRegister.Q),  // P+Q -> dest
        Instruction.ALU(false, WritableRegister.P, WritableRegister.Q, AluOperation.A_PLUS_B),  // P -> dest
    )

    @Test
    fun linkedCalls() {
        val ac = ObjectFile(
            imports = listOf("b"),
            exports = mapOf("a" to 2u, "c" to 0u),

            listOf(
                // c
                Instruction.ALU(false, WritableRegister.A, WritableRegister.B, AluOperation.A_PLUS_B), // <- 0
                Instruction.HLT,

                // a
                *resolveIpRelAddr(2),
                Instruction.CMP(true, JumpCondition(0b111), WritableRegister.P)
            ).map(Instruction::code)
        )

        val b = ObjectFile(
            imports = listOf("const", "c"),
            exports = mapOf("b" to 0u),

            listOf(
                // b
                Instruction.SET(true, WritableRegister.A, 0x01),
                Instruction.SET(false, WritableRegister.A, 0x20),
                *resolveIpRelAddr(3),  // P -> const, len=6
                Instruction.MEM(false, WritableRegister.P, WritableRegister.B),
                *resolveIpRelAddr(9),  // P -> c
                Instruction.CMP(true, JumpCondition(0b111), WritableRegister.P)
            ).map(Instruction::code)
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