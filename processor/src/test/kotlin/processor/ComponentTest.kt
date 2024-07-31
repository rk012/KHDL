package processor

import common.*
import hdl.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ComponentTest {
    @Test
    fun programCounter() {
        val clk = Clock()
        val counter = ProgramCounter(clk, 4)

        val src = BusSource(6)
        counter.input + counter.w + counter.en bind src.outputBus

        TestBus(counter.out).testLines(
            listOf(
                0b0000_1_0__0000,
                0b1111_0_0__0000,
                0b1111_0_1__0000,
                0b1111_0_1__0001,
                0b1111_0_1__0010,
                0b1111_0_1__0011,
                0b1110_1_1__0100,
                0b1111_0_1__1110,
                0b1001_0_1__1111,
                0b1001_0_1__0000,
            ),
            clk, src
        )
    }

    @Test
    fun fetchState() {
        val clk = Clock()
        val chip = FetchState(clk)
        val src = BusSource(1)
        chip.enable bind src.outputBus[0]

        TestBus(listOf(chip.isFetch)).testLines(
            listOf(
                0b0_0,
                0b1_1,
                0b1_0,
                0b1_1,
                0b0_0,
                0b1_1,
                0b1_0,
                0b0_0,
                0b1_1
            ), clk, src
        )
    }

    @Test
    fun cpuRegisters() {
        val clk = Clock()
        val chip = CpuRegisters(clk)
        val src = BusSource(14)
        (chip.d.subList(0, 4) + chip.wAddr + chip.w + chip.addrA + chip.addrB) bind src.outputBus
        chip.d.subList(4, 16) bind BusSource(12).apply { value = List(12) { false } }.outputBus

        TestBus(chip.a.subList(0, 4) + chip.b.subList(0, 4)).testLines(
            listOf(
                //   d  wa w   a   b     a    b
                0b1010_000_0_000_000__0000_0000,
                0b1010_000_1_000_000__0000_0000,
                0b1111_001_1_000_000__1010_1010,
                0b0101_110_1_001_000__1111_1010,
                0b0000_000_0_000_110__1010_0101,
                0b0001_110_1_000_110__1010_0101,
                0b0001_110_0_000_110__1010_0001,
            ),
            clk, src
        )
    }

    private fun Int.trim3() = this and 0b111

    @Test
    fun jmpCmp() {
        val numPairs = (-4..3).flatMap { a -> (-4..3).map { b -> a to b } }
        val jmpConds = (0..7).map { JumpCondition(it) }

        val alu = ALU(3)
        val cmp = JmpCmp()

        val controlPins = listOf(alu.za, alu.na, alu.zb, alu.nb, alu.f, alu.no)
        controlPins bind BusSource(6).apply { setN(AluOperation.A_MINUS_B.opcode) }.outputBus
        cmp.eq bind alu.zero
        cmp.neg bind alu.neg
        cmp.overflow bind alu.overflow

        val src = BusSource(9)
        (alu.a + alu.b + cmp.cond) bind src.outputBus

        val expects = numPairs.flatMap { (a, b) -> jmpConds.map { c -> if (c.shouldJump(a-b)) 1 else 0 } }
        val inputs =
            numPairs.flatMap { (a, b) ->
            jmpConds.map { c ->
                (a.trim3() shl 6) or (b.trim3() shl 3) or c.code
            } }

        TestBus(listOf(cmp.output)).test(inputs, expects, Clock(), src)
    }

    private val OutputBus.n get() = peekInt()

    private fun testInstruction(
        instruction: Instruction, src: BusSource,
        opFlags: OutputBus, ab: OutputPin, cond: OutputBus,
        xReg: OutputBus, regA: OutputBus, regB: OutputBus,
        aluOp: OutputBus, iVal: OutputBus
    ) {
        src.setN(instruction.code)

        when (instruction) {
            Instruction.HLT -> assertEquals(0b10000000, opFlags.n)
            Instruction.NOP -> assertEquals(0b01000000, opFlags.n)
            is Instruction.MOV -> {
                assertEquals(0b00100000, opFlags.n)
                assertEquals(instruction.src.xCode, xReg.n)
                assertEquals(instruction.dest.code, regB.n)
            }
            is Instruction.SET -> {
                assertEquals(0b00010000, opFlags.n)
                assertEquals(instruction.sideFlag, ab.peek())
                assertEquals(instruction.dest.code, regA.n)
                assertEquals(instruction.value, iVal.n)
            }
            is Instruction.CMP -> {
                assertEquals(0b00001000, opFlags.n)
                assertEquals(instruction.jmp, ab.peek())
                assertEquals(instruction.cond, JumpCondition(cond.n))
                assertEquals(instruction.reg.code, regB.n)
            }
            is Instruction.ALU -> {
                assertEquals(0b00000100, opFlags.n)
                assertEquals(instruction.q, ab.peek())
                assertEquals(instruction.a.code, regA.n)
                assertEquals(instruction.b.code, regB.n)
                assertEquals(instruction.op.opcode, aluOp.n)
            }
            is Instruction.MEM -> {
                assertEquals(0b00000010, opFlags.n)
                assertEquals(instruction.w, ab.peek())
                assertEquals(instruction.addr.code, regA.n)
                assertEquals(instruction.d.code, regB.n)
            }
            is Instruction.IO -> {
                assertEquals(0b00000001, opFlags.n)
                assertEquals(instruction.w, ab.peek())
                assertEquals(instruction.addr.code, regA.n)
                assertEquals(instruction.d.code, regB.n)
            }
        }
    }

    @Test
    fun instructionDecoder() {
        val instructions = listOf(
            Instruction.NOP,
            Instruction.HLT,
            Instruction.MOV(ReadOnlyRegister.IP, WritableRegister.A),
            Instruction.MOV(WritableRegister.B, WritableRegister.C),
            Instruction.SET(true, WritableRegister.B,254),
            Instruction.CMP(false, JumpCondition(eq=false, lt=true, gt=true), WritableRegister.C),
            Instruction.CMP(true, JumpCondition(eq=false, lt=true, gt=true), WritableRegister.C),
            Instruction.ALU(false, WritableRegister.D, WritableRegister.Q, AluOperation.OR),
            Instruction.MEM(true, WritableRegister.SP, WritableRegister.P),
            Instruction.IO(true, WritableRegister.A, WritableRegister.B)
        )

        val chip = InstructionDecoder()
        val src = BusSource(16)
        chip.instruction bind src.outputBus

        instructions.forEach {
            testInstruction(
                it, src,
                opFlags = listOf(chip.hlt, chip.nop, chip.mov, chip.set, chip.jmp, chip.alu, chip.mem, chip.io),
                ab=chip.ab, cond=chip.cond,
                xReg=chip.xReg, regA=chip.regA, regB=chip.regB,
                aluOp=chip.aluOp, iVal=chip.iVal
            )
        }
    }
}