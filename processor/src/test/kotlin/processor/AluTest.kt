package processor

import common.AluOperation
import hdl.BusSource
import hdl.Clock
import hdl.TestBus
import hdl.bind
import org.junit.jupiter.api.Disabled
import kotlin.test.Test

class AluTest {
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
            val res = instruction.computation(a, b)
            val overflow = (instruction.opcode and 0b000010) != 0 && res !in (-8..7)
            var n = (res.trim4() shl 1) or if (overflow) 1 else 0
            val isNeg = n >= 0b10000
            val isZero = (n shr 1) == 0
            if (!flags) return@map n

            n = n shl 2

            if (isZero) n = n or 0b01
            if (isNeg) n = n or 0b10

            n
        }

        runCatching {
            testBus.test(inputs, expects, Clock(), src)
        }.onFailure {
            if (it !is IllegalArgumentException) throw it

            throw IllegalArgumentException(it.message + "\nInstruction: $instruction").apply { stackTrace = it.stackTrace }
        }
    }

    @Test
    @Disabled("Expensive ALU Test")
    fun mainUnit() {
        val unit = ALU.MainUnit(4)
        val src = BusSource(6+4+4)
        (listOf(unit.za, unit.na, unit.zb, unit.nb, unit.f, unit.no) + unit.a + unit.b) bind src.outputBus
        val testBus = TestBus(unit.out + listOf(unit.overflow))

        AluOperation.entries.filter { it !in altInstructions }.forEach {
            testInstruction(it, testBus, src, false)
        }
    }

    @Test
    @Disabled("Expensive ALU Test")
    fun alu() {
        val alu = ALU(4)
        val src = BusSource(6+4+4)
        (listOf(alu.za, alu.na, alu.zb, alu.nb, alu.f, alu.no) + alu.a + alu.b) bind src.outputBus
        val testBus = TestBus(alu.out + listOf(alu.overflow, alu.neg, alu.zero))

        AluOperation.entries.forEach { testInstruction(it, testBus, src, true) }
    }
}