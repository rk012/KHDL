package assembler.instructions

import assembler.AsmCommand
import assembler.InstructionBlock
import assembler.Macro
import assembler.cpuInstructions
import common.*

fun hlt() = cpuInstructions(CpuInstruction.HLT)
fun nop() = cpuInstructions(CpuInstruction.NOP)

fun mov(src: Register, dest: WritableRegister) = cpuInstructions(CpuInstruction.MOV(src, dest))
fun mov(p: Pair<Register, WritableRegister>) = mov(p.first, p.second)


fun set(reg: WritableRegister, value: Macro<Int>) = AsmCommand { cfg ->
    listOf(
        InstructionBlock(2) { ctx ->
            listOf(
                CpuInstruction.SET(true, reg, (value.eval(cfg, ctx) shr 8) and 0xFF),
                CpuInstruction.SET(false, reg, value.eval(cfg, ctx) and 0xFF)
            ).map(CpuInstruction::code)
        }
    )
}

fun set(reg: WritableRegister, value: Int) = cpuInstructions(
    CpuInstruction.SET(true, reg, (value shr 8) and 0xFF),
    CpuInstruction.SET(false, reg, value and 0xFF)
)


fun cmp(cond: JumpCondition, reg: WritableRegister) = cpuInstructions(CpuInstruction.CMP(false, cond, reg))


fun jmp(dest: WritableRegister) = cpuInstructions(CpuInstruction.CMP(true, JumpCondition(0b111), dest))

fun je(dest: WritableRegister) = cpuInstructions(CpuInstruction.CMP(true, JumpCondition(eq = true), dest))
fun jl(dest: WritableRegister) = cpuInstructions(CpuInstruction.CMP(true, JumpCondition(lt = true), dest))
fun jg(dest: WritableRegister) = cpuInstructions(CpuInstruction.CMP(true, JumpCondition(gt = true), dest))

fun jne(dest: WritableRegister) = cpuInstructions(
    CpuInstruction.CMP(true, JumpCondition(gt = true, lt = true), dest)
)

fun jle(dest: WritableRegister) = cpuInstructions(
    CpuInstruction.CMP(true, JumpCondition(lt = true, eq = true), dest)
)

fun jge(dest: WritableRegister) = cpuInstructions(
    CpuInstruction.CMP(true, JumpCondition(gt = true, eq = true), dest)
)


fun aluP(a: WritableRegister, b: WritableRegister, op: AluOperation) =
    cpuInstructions(CpuInstruction.ALU(false, a, b, op))

fun aluQ(a: WritableRegister, b: WritableRegister, op: AluOperation) =
    cpuInstructions(CpuInstruction.ALU(true, a, b, op))


fun memRead(addr: WritableRegister, dest: WritableRegister) = cpuInstructions(CpuInstruction.MEM(false, addr, dest))
fun memWrite(addr: WritableRegister, d: WritableRegister) = cpuInstructions(CpuInstruction.MEM(true, addr, d))

fun ioRead(addr: WritableRegister, dest: WritableRegister) = cpuInstructions(CpuInstruction.IO(false, addr, dest))
fun ioWrite(addr: WritableRegister, d: WritableRegister) = cpuInstructions(CpuInstruction.IO(true, addr, d))
