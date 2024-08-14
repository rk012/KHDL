package compiler

import VirtualMachine
import assembler.AsmConfig
import assembler.asm
import assembler.assemble
import assembler.instructions.*
import assembler.link
import common.CpuInstruction
import common.WritableRegister
import kotlin.test.Test
import kotlin.test.assertEquals

class MultDivTest {
    @Test
    fun mult() {
        val exe = asm {
            +set(WritableRegister.SP, 0)
            +mov(WritableRegister.SP to WritableRegister.BP)
            +push(WritableRegister.A)
            +push(WritableRegister.B)
            +callLocal(MultiplyFn.name)
            +hlt()

            +MultiplyFn.name
            addAll(MultiplyFn.assembly)
        }.let { link(null, assemble(AsmConfig(), it)) }

        val vm = VirtualMachine(exe.bytecode)

        val pairs = (-10..10).flatMap { a -> (-10..10).map { b -> a to b } }

        pairs.forEach { (a, b) ->
            vm.reset()
            vm.runInstructions(listOf (
                CpuInstruction.SET(true, WritableRegister.A, (a shr 8) and 0xFF),
                CpuInstruction.SET(true, WritableRegister.B, (b shr 8) and 0xFF),
                CpuInstruction.SET(false, WritableRegister.A, a and 0xFF),
                CpuInstruction.SET(false, WritableRegister.B, b and 0xFF)
            ))
            vm.runUntilHalt()
            assertEquals((a*b).toShort(), vm.debugRegister(WritableRegister.A))
        }
    }
}