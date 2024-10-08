package processor

import common.*
import hdl.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CpuTest {
    private lateinit var clk: Clock
    private lateinit var cpu: CPU

    private lateinit var enable: PinSource
    private lateinit var memData: BusSource
    private lateinit var ioData: BusSource
    private lateinit var dbgMode: BusSource
    private lateinit var dbgIn: BusSource

    private lateinit var hlt: OutputPin
    private lateinit var dataOut: OutputBus
    private lateinit var addrOut: OutputBus
    private lateinit var memWrite: OutputPin
    private lateinit var ioWrite: OutputPin
    private lateinit var dbgOut: OutputBus
    private lateinit var execActive: OutputPin

    @BeforeTest
    fun setup() {
        clk = Clock()
        cpu = CPU(clk)

        enable = PinSource()
        memData = BusSource(16)
        ioData = BusSource(16)
        dbgMode = BusSource(2)
        dbgIn = BusSource(16)

        cpu.enable bind enable
        cpu.memData bind memData.outputBus
        cpu.ioData bind ioData.outputBus
        cpu.dbgMode bind dbgMode.outputBus
        cpu.dbgIn bind dbgIn.outputBus


        hlt = cpu.hlt
        dataOut = cpu.dataOut
        addrOut = cpu.addrOut
        memWrite = cpu.memWrite
        ioWrite = cpu.ioWrite
        dbgOut = cpu.dbgOut
        execActive = cpu.execActive
    }

    private val CpuInstruction.ic: Int get() = code.toInt()

    private fun readReg(reg: Register): Int {
        dbgMode.setN(0b01)
        dbgIn.setN(reg.xCode)
        val regVal = dbgOut.peekInt()
        dbgMode.setN(0b00)
        return regVal
    }

    private fun assertReadOnly() {
        assert(!memWrite.peek())
        assert(!ioWrite.peek())
    }

    private fun assertMemRead(addr: Int, res: Int) {
        assertEquals(addr, addrOut.peekInt())
        assert(!memWrite.peek())
        memData.setN(res)
    }

    private fun runInstructions(vararg instructions: CpuInstruction) {
        assert(!execActive.peek() && enable.value)
        instructions.forEach {
            memData.setN(it.ic)
            clk.pulse()
            clk.pulse()
        }
    }

    private fun setReg(reg: WritableRegister, value: Int) = runInstructions(
        CpuInstruction.SET(true, reg, value shr 8),
        CpuInstruction.SET(false, reg, value and 0xFF)
    )

    @Test
    fun enable() {
        enable.value = false
        memData.setN(CpuInstruction.NOP.ic)

        assert(!execActive.peek())
        assertReadOnly()

        repeat(10) {
            clk.pulse()
            assertReadOnly()
            assert(!execActive.peek())
        }

        enable.value = true
        assert(!execActive.peek())

        clk.pulse()
        assert(execActive.peek())
    }

    @Test
    fun hlt() {
        enable.value = true
        memData.setN(CpuInstruction.HLT.ic)

        assert(!execActive.peek())
        assert(!hlt.peek())
        assertReadOnly()
        clk.pulse()
        assert(execActive.peek())
        assert(hlt.peek())
        assertReadOnly()
        clk.pulse()
        assert(!execActive.peek())
        assert(!hlt.peek())
        assertReadOnly()
    }

    @Test
    fun nop() {
        enable.value = true
        memData.setN(CpuInstruction.NOP.ic)

        repeat(5) { i ->
            assert(!execActive.peek())
            assertReadOnly()
            assertEquals(i, addrOut.peekInt())

            clk.pulse()

            assert(execActive.peek())
            assertReadOnly()
            assertEquals(i+1, readReg(ReadOnlyRegister.IP))

            clk.pulse()
        }
    }

    @Test
    fun setMov() {
        enable.value = true
        assertMemRead(0, CpuInstruction.SET(true, WritableRegister.A, 0x12).ic)
        clk.pulse()
        clk.pulse()
        assertMemRead(1, CpuInstruction.SET(false, WritableRegister.A, 0x34).ic)
        clk.pulse()
        clk.pulse()
        assertEquals(0x1234, readReg(WritableRegister.A))
        assertMemRead(2, CpuInstruction.MOV(WritableRegister.A, WritableRegister.B).ic)
        clk.pulse()
        clk.pulse()
        assertEquals(0x1234, readReg(WritableRegister.B))
        assertEquals(0x1234, readReg(WritableRegister.A))
    }

    @Test
    fun setMemIO() {
        enable.value = true
        assertMemRead(0, CpuInstruction.SET(true, WritableRegister.A, 0xDE).ic)
        clk.pulse()
        clk.pulse()
        assertMemRead(1, CpuInstruction.SET(false, WritableRegister.A, 0xEF).ic)
        clk.pulse()
        clk.pulse()
        assertMemRead(2, CpuInstruction.SET(true, WritableRegister.B, 0x56).ic)
        clk.pulse()
        clk.pulse()
        assertMemRead(3, CpuInstruction.SET(false, WritableRegister.B, 0x78).ic)
        clk.pulse()
        clk.pulse()
        assertMemRead(4, CpuInstruction.MEM(true, WritableRegister.A, WritableRegister.B).ic)
        clk.pulse()
        assertEquals(0x5678, dataOut.peekInt())
        assertEquals(0xDEEF, addrOut.peekInt())
        assert(memWrite.peek())
        assert(!ioWrite.peek())
        clk.pulse()
        assertMemRead(5, CpuInstruction.IO(false, WritableRegister.A, WritableRegister.B).ic)
        clk.pulse()
        assertEquals(0xDEEF, addrOut.peekInt())
        assertReadOnly()
        ioData.setN(0x9ABC)
        clk.pulse()
        assertMemRead(6, CpuInstruction.IO(true, WritableRegister.A, WritableRegister.B).ic)
        clk.pulse()
        assertEquals(0x9ABC, dataOut.peekInt())
        assertEquals(0xDEEF, addrOut.peekInt())
        assert(!memWrite.peek())
        assert(ioWrite.peek())
    }

    @Test
    fun alu() {
        enable.value = true

        setReg(WritableRegister.A, 0x105F)
        setReg(WritableRegister.B, 0x2031)

        runInstructions(
            CpuInstruction.ALU(false, WritableRegister.A, WritableRegister.B, AluOperation.A_PLUS_B),
            CpuInstruction.ALU(true, WritableRegister.A, WritableRegister.B, AluOperation.A_MINUS_B),
        )

        assertEquals(0x105F + 0x2031, readReg(WritableRegister.P))
        assertEquals((0x105F - 0x2031) and ((1 shl 16) - 1), readReg(WritableRegister.Q))
        assertEquals(0b010, readReg(ReadOnlyRegister.FLAGS))

        setReg(WritableRegister.A, 0x7FFF)
        setReg(WritableRegister.B, 0x0001)

        runInstructions(
            CpuInstruction.ALU(false, WritableRegister.A, WritableRegister.B, AluOperation.A_PLUS_B),
        )

        assertEquals(0x8000, readReg(WritableRegister.P))
        assertEquals(0b011, readReg(ReadOnlyRegister.FLAGS))
    }

    @Test
    fun cmp() {
        enable.value = true

        setReg(WritableRegister.A, 0x7FFF)
        setReg(WritableRegister.B, 0x8000)
        runInstructions(
            CpuInstruction.ALU(false, WritableRegister.A, WritableRegister.B, AluOperation.A_MINUS_B),
            CpuInstruction.CMP(false, JumpCondition(gt=true), WritableRegister.P),
            CpuInstruction.CMP(false, JumpCondition(lt=true), WritableRegister.Q),
            CpuInstruction.CMP(false, JumpCondition(eq=true), WritableRegister.C),
        )

        assertEquals(0x0001, readReg(WritableRegister.P))
        assertEquals(0x0000, readReg(WritableRegister.Q))
        assertEquals(0x0000, readReg(WritableRegister.C))
    }

    @Test
    fun jmp() {
        enable.value = true

        setReg(WritableRegister.A, 0xABCD)
        memData.setN(CpuInstruction.CMP(true, JumpCondition(0b111), WritableRegister.A).ic)
        clk.pulse()
        clk.pulse()
        assertMemRead(0xABCD, CpuInstruction.NOP.ic)
        clk.pulse()
        assert(execActive.peek())
        clk.pulse()
        assertMemRead(0xABCE, CpuInstruction.CMP(false, JumpCondition(0b111), WritableRegister.A).ic)
        clk.pulse()
        clk.pulse()
        assertMemRead(0xABCF, CpuInstruction.NOP.ic)
    }

    @Test
    fun dbgMem() {
        enable.value = true
        memData.setN(CpuInstruction.NOP.ic)
        clk.pulse()

        dbgMode.setN(0b10)
        dbgIn.setN(0x1234)
        assertMemRead(0x1234, 0x5678)
        assertEquals(0x5678, dbgOut.peekInt())
    }

    @Test
    fun dbgInstruction() {
        enable.value = true
        memData.setN(CpuInstruction.NOP.ic)
        clk.pulse()
        assertEquals(1, readReg(ReadOnlyRegister.IP))

        dbgMode.setN(0b11)
        dbgIn.setN(CpuInstruction.SET(true, WritableRegister.A, 0x12).ic)
        clk.pulse()
        clk.pulse()
        dbgIn.setN(CpuInstruction.SET(false, WritableRegister.A, 0x34).ic)
        clk.pulse()
        clk.pulse()

        dbgIn.setN(0)
        dbgMode.setN(0b00)

        assertEquals(1, readReg(ReadOnlyRegister.IP))
        assertEquals(0x1234, readReg(WritableRegister.A))
    }

    @Test
    fun rstTest() {
        enable.value = true
        memData.setN(CpuInstruction.NOP.ic)

        repeat(5) {
            clk.pulse()
            clk.pulse()
        }

        assertEquals(5, readReg(ReadOnlyRegister.IP))

        enable.value = false
        clk.pulse()
        enable.value = true

        assertEquals(0, readReg(ReadOnlyRegister.IP))

        repeat(5) {
            clk.pulse()
            clk.pulse()
        }

        assertEquals(5, readReg(ReadOnlyRegister.IP))
    }
}