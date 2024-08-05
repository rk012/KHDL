import common.*
import hardware.BusSwitch
import hardware.Nor
import hardware.Or
import hardware.VirtualRam
import hdl.*
import processor.Bootloader
import processor.CPU

class HardwareComputer(override val rom: List<Int>) : Computer {
    private val clk = Clock()

    override val ioController = HardwareIOController(clk, 16, 16)

    private val ram = VirtualRam(clk, 16, 16)
    private val cpu = CPU(clk)
    private val bootloader = Bootloader(clk, rom, 16)

    private val rst = PinSource(false)

    private val dbgMode = BusSource(2).apply { setN(0) }
    private val dbgData = BusSource(16)

    private val active = cpu.execActive
    private val dbgOut = cpu.dbgOut
    private val hlt = cpu.hlt

    private val addrSwitch = BusSwitch(16)
    private val dataSwitch = BusSwitch(16)
    private val writeOr = Or()

    private val enNor = Nor()

    init {
        ram.w bind writeOr.out
        writeOr.a bind bootloader.w
        writeOr.b bind cpu.memWrite

        ram.input bind dataSwitch.out
        ram.addr bind addrSwitch.out
        dataSwitch.a bind cpu.dataOut
        addrSwitch.a bind cpu.addrOut
        dataSwitch.b bind bootloader.d
        addrSwitch.b bind bootloader.addr
        dataSwitch.select bind bootloader.w
        addrSwitch.select bind bootloader.w

        cpu.memData bind ram.out

        ioController.d bind cpu.dataOut
        ioController.addr bind cpu.addrOut
        ioController.w bind cpu.ioWrite
        cpu.ioData bind ioController.out

        enNor.a bind rst
        enNor.b bind bootloader.w
        cpu.enable bind enNor.out

        bootloader.rst bind rst

        cpu.dbgMode bind dbgMode.outputBus
        cpu.dbgIn bind dbgData.outputBus
    }

    private fun runBootloader() {
        while (!active.peek(clk.nonce)) clk.pulse()
    }

    init {
        runBootloader()
    }

    override fun runUntilHalt() {
        clk.pulse()
        while (!hlt.peek(clk.nonce)) clk.pulse()
    }

    override fun runNextInstruction() {
        clk.pulse()
        clk.pulse()
    }

    override fun reset() {
        rst.value = true
        clk.pulse()
        rst.value = false
        runBootloader()
    }

    override fun debugRegister(register: Register): Int {
        dbgMode.setN(0b01)
        dbgData.setN(register.xCode)
        val res = dbgOut.peekInt()
        dbgMode.setN(0b00)
        return res
    }

    override fun debugMemory(address: Int): Int {
        dbgMode.setN(0b10)
        dbgData.setN(address)
        val res = dbgOut.peekInt()
        dbgMode.setN(0b00)
        return res
    }

    override fun runInstructions(instructions: List<Instruction>) {
        val ip = debugRegister(ReadOnlyRegister.IP) - 1
        val q = debugRegister(WritableRegister.Q)

        dbgMode.setN(0b11)

        val wrappedInstructions = instructions + listOf(
            Instruction.SET(true, WritableRegister.Q, ip shr 8),
            Instruction.SET(false, WritableRegister.Q, ip and 0xFF),
            Instruction.CMP(true, JumpCondition(0b111), WritableRegister.Q),
            Instruction.SET(true, WritableRegister.Q, q shr 8),
            Instruction.SET(false, WritableRegister.Q, q and 0xFF),
        )

        wrappedInstructions.forEach {
            dbgData.setN(it.code)
            clk.pulse()
            clk.pulse()
        }

        dbgMode.setN(0b00)
    }
}