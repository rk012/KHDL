package processor

import common.*
import hdl.Clock
import hdl.PinSource
import hdl.peekInt
import s
import kotlin.test.Test
import kotlin.test.assertEquals

class BootloaderTest {
    @Test
    fun bootloader() {
        val rom = listOf(
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
        ).map { it.code.toInt() } + listOf(1, 2, 3)

        val clk = Clock()
        val bootloader = Bootloader(clk, rom, 16)

        val rst = PinSource()
        bootloader.rst bind rst

        rom.forEachIndexed { i, line ->
            assertEquals(i, bootloader.addr.peekInt())
            assertEquals(line.s, bootloader.d.peekInt().s)
            assert(bootloader.w.peek())

            clk.pulse()
        }

        assert(!bootloader.w.peek())

        repeat(5) {
            clk.pulse()
            assert(!bootloader.w.peek())
        }

        rst.value = true

        repeat(5) {
            clk.pulse()
            assert(!bootloader.w.peek())
        }

        rst.value = false

        rom.forEachIndexed { i, line ->
            assertEquals(i, bootloader.addr.peekInt())
            assertEquals(line.s, bootloader.d.peekInt().s)
            assert(bootloader.w.peek())

            clk.pulse()
        }
    }
}