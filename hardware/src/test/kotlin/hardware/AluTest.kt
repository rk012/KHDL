package hardware

import common.AluOperation
import hdl.BusSource
import hdl.Clock
import hdl.TestBus
import hdl.bind
import kotlin.test.Test

class AluTest {
    @Test
    fun adder() {
        val adder = FullAdder()

        val src = BusSource(3)

        listOf(adder.a, adder.b, adder.carryIn) bind src.outputBus

        TestBus(listOf(adder.carryOut, adder.out)).testLines(
            listOf(
                0b000__00,
                0b001__01,
                0b010__01,
                0b011__10,
                0b100__01,
                0b101__10,
                0b110__10,
                0b111__11
            ),
            Clock(),
            src
        )
    }

    @Test
    fun isZero() {
        val isZero = IsZero(4)
        val src = BusSource(4)
        isZero.inputs bind src.outputBus

        TestBus(listOf(isZero.out)).testLines(
            listOf(
                0b0000__1,
                0b0001__0,
                0b0010__0,
                0b0011__0,
                0b0100__0,
                0b0101__0,
                0b0110__0,
                0b0111__0,
                0b1000__0,
                0b1001__0,
                0b1010__0,
                0b1011__0,
                0b1100__0,
                0b1101__0,
                0b1110__0,
                0b1111__0
            ),
            Clock(),
            src
        )
    }

    @Test
    fun controlBits() {
        val chip = ALU.ControlBits()
        val src = BusSource(6)
        listOf(chip.za, chip.na, chip.zb, chip.nb, chip.f, chip.no) bind src.outputBus

        TestBus(listOf(chip.c0, chip.c1, chip.c2)).testLines(
            listOf(
                0b000000_000,
                0b101010_111,
                0b111010_000,
                0b001010_011,
                0b100010_101,
            ),
            Clock(),
            src
        )
    }

    private val altInstructions = setOf(AluOperation.XOR, AluOperation.A_SHR, AluOperation.B_SHR)

    private val aluTestNums = (-8..7).flatMap { a -> (-8..7).map { b -> a to b } }

    private fun Int.trim4() = this and ((1 shl 4) - 1)

    private fun testInstruction(instruction: AluOperation, testBus: TestBus, src: BusSource, flags: Boolean) {
        val inputs = aluTestNums.map { (a, b) -> (instruction.opcode shl 8) or (a.trim4() shl 4) or b.trim4() }
        val expects = aluTestNums.map { (a, b) ->
            var n = instruction.computation(a, b).trim4()
            val isNeg = n >= 0b1000
            if (!flags) return@map n

            n = n shl 2

            if (n == 0) n = 0b01
            if (isNeg) n = n or 0b10

            n
        }

        runCatching {
            testBus.test(inputs, expects, Clock(), src)
        }.onFailure {
            if (it !is IllegalArgumentException) throw it

            throw IllegalArgumentException(it.message + "\nInstruction: $instruction")
        }
    }

    @Test
    fun mainUnit() {
        val unit = ALU.MainUnit(4)
        val src = BusSource(6+4+4)
        (listOf(unit.za, unit.na, unit.zb, unit.nb, unit.f, unit.no) + unit.a + unit.b) bind src.outputBus
        val testBus = TestBus(unit.out)

        AluOperation.entries.filter { it !in altInstructions }.forEach {
            testInstruction(it, testBus, src, false)
        }
    }

    @Test
    fun alu() {
        val alu = ALU(4)
        val src = BusSource(6+4+4)
        (listOf(alu.za, alu.na, alu.zb, alu.nb, alu.f, alu.no) + alu.a + alu.b) bind src.outputBus
        val testBus = TestBus(alu.out + listOf(alu.neg, alu.zero))

        AluOperation.entries.forEach { testInstruction(it, testBus, src, true) }
    }
}