import common.*
import java.util.EnumMap
import kotlin.experimental.and
import kotlin.experimental.or

class VirtualMachine(override val rom: Bytecode) : Computer {
    private class VirtualIOController : IOController() {
        val readBuf = ShortArray(1 shl 16)
        val writeBuf = ShortArray(1 shl 16)

        override fun readOutput(port: UShort) = readBuf[port.toInt()]

        override fun writeInput(port: UShort, value: Short) {
            writeBuf[port.toInt()] = value
        }

        fun update() {
            devices.forEach(IODevice::update)
        }
    }

    private val controller = VirtualIOController()
    override val ioController: IOController = controller


    private val ram = ShortArray(1 shl 16)

    private val registers: MutableMap<WritableRegister, Short> = EnumMap(WritableRegister::class.java)

    init {
        WritableRegister.entries.forEach { registers[it] = 0 }
    }

    private var ip = 0
    private var flags = 0.s
    private var halt = false

    private fun readReg(register: Register) = when (register) {
        is WritableRegister -> registers[register] ?: 0
        is ReadOnlyRegister -> when (register) {
            ReadOnlyRegister.IP -> ip.s
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

                registers[instruction.dest] = (readReg(instruction.dest) and mask.s) or part.s
            }

            is Instruction.CMP -> {
                val overflow = (flags and 0b100) != 0.s

                val lt = overflow xor ((flags and 0b001) != 0.s)
                val eq = (flags and 0b010) != 0.s
                val gt = !lt && !eq

                val res = if (instruction.cond.let { it.eq && eq || it.lt && lt || it.gt && gt }) 1 else 0

                if (instruction.jmp) {
                    if (res == 1) {
                        ip = readReg(instruction.reg).toUShort().toInt()
                    }
                } else {
                    registers[instruction.reg] = res.s
                }
            }

            is Instruction.ALU -> {
                val dest = if (instruction.q) WritableRegister.Q else WritableRegister.P
                val intRes = instruction.op.computation(readReg(instruction.a).toInt(), readReg(instruction.b).toInt())
                val res = intRes.s

                val overflow = (instruction.op.opcode and 0b000010) != 0 && res.toInt() != intRes
                var flag = if (overflow) 0b100 else 0

                if (res < 0) flag = flag or 0b001
                if (res == 0.s) flag = flag or 0b010

                registers[dest] = res
                flags = flag.s
            }

            is Instruction.MEM -> if (instruction.w) {
                ram[readReg(instruction.addr).toUShort().toInt()] = readReg(instruction.d)
            } else {
                registers[instruction.d] = ram[readReg(instruction.addr).toUShort().toInt()]
            }

            is Instruction.IO -> if (instruction.w) {
                controller.readBuf[readReg(instruction.addr).toUShort().toInt()] = readReg(instruction.d)
            } else {
                registers[instruction.d] = controller.writeBuf[readReg(instruction.addr).toUShort().toInt()]
            }
        }

        controller.update()
    }

    override fun runNextInstruction() = exec(Instruction.parse(ram[ip++].toInt()))

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