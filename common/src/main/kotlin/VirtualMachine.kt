import common.*
import java.util.EnumMap

class VirtualMachine(override val rom: Bytecode) : Computer {
    private class VirtualIOController : IOController() {
        val readBuf = IntArray(1 shl 16)
        val writeBuf = IntArray(1 shl 16)

        override fun readOutput(port: Int) = readBuf[port]

        override fun writeInput(port: Int, value: Int) {
            writeBuf[port] = value
        }

        fun update() {
            devices.forEach(IODevice::update)
        }
    }

    private val controller = VirtualIOController()
    override val ioController: IOController = controller


    private val ram = IntArray(1 shl 16)

    private val registers: MutableMap<WritableRegister, Int> = EnumMap(WritableRegister::class.java)

    init {
        WritableRegister.entries.forEach { registers[it] = 0 }
    }

    private var ip = 0
    private var flags = 0
    private var halt = false

    private fun Int.trim16() = toShort().toInt()

    private fun readReg(register: Register) = when (register) {
        is WritableRegister -> registers[register] ?: 0
        is ReadOnlyRegister -> when (register) {
            ReadOnlyRegister.IP -> ip
            ReadOnlyRegister.FLAGS -> flags
        }
    }

    override fun reset() {
        ip = 0
        halt = false

        rom.forEachIndexed { i, v -> ram[i] = v }
    }

    init {
        reset()
    }


    private fun exec(instruction: Instruction) {
        when (instruction) {
            Instruction.HLT -> halt = true
            Instruction.NOP -> Unit

            is Instruction.MOV -> registers[instruction.dest] = readReg(instruction.src)

            is Instruction.SET -> {
                val part = if (instruction.sideFlag) instruction.value shl 8 else instruction.value
                val mask = if (instruction.sideFlag) 0x00FF else 0xFF00

                registers[instruction.dest] = (readReg(instruction.dest) and mask) or part
            }

            is Instruction.CMP -> {
                val overflow = (flags and 0b100) != 0

                val lt = overflow xor ((flags and 0b001) != 0)
                val eq = (flags and 0b010) != 0
                val gt = !lt && !eq

                val res = if (instruction.cond.let { it.eq && eq || it.lt && lt || it.gt && gt }) 1 else 0

                if (instruction.jmp) {
                    if (res == 1) {
                        ip = readReg(instruction.reg)
                    }
                } else {
                    registers[instruction.reg] = res
                }
            }

            is Instruction.ALU -> {
                val dest = if (instruction.q) WritableRegister.Q else WritableRegister.P
                var res = instruction.op.computation(readReg(instruction.a), readReg(instruction.b))

                val overflow = (instruction.op.opcode and 0b000010) != 0 && res !in (-32768..32767)
                var flag = if (overflow) 0b100 else 0

                res = res.trim16()
                if (res < 0) flag = flag or 0b001
                if (res == 0) flag = flag or 0b010

                registers[dest] = res
                flags = flag
            }

            is Instruction.MEM -> if (instruction.w) {
                ram[readReg(instruction.addr)] = readReg(instruction.d).trim16()
            } else {
                registers[instruction.d] = ram[readReg(instruction.addr)]
            }

            is Instruction.IO -> if (instruction.w) {
                controller.readBuf[readReg(instruction.addr)] = readReg(instruction.d)
            } else {
                registers[instruction.d] = controller.writeBuf[readReg(instruction.addr)].trim16()
            }
        }

        controller.update()
    }

    override fun runNextInstruction() = exec(Instruction.parse(ram[ip++]))

    override fun runUntilHalt() {
        while (!halt) {
            runNextInstruction()
        }

        halt = false
    }


    override fun debugRegister(register: Register) = readReg(register)

    override fun debugMemory(address: Int) = ram[address]

    override fun runInstructions(instructions: List<Instruction>) {
        instructions.forEach(::exec)
    }
}