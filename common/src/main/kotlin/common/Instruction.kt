package common

sealed class Instruction(opcode: Int, vararg masks: Int) {
    companion object {
        private fun boolMask(flag: Boolean) = (if (flag) 1 else 0) shl 12

        fun parse(code: Int): Instruction {
            val op = code shr 13
            val flag = ((code shr 12) and 1) != 0
            val xReg = (code shr 9) and 0b1111
            val regA = xReg and 0b111
            val regB = (code shr 6) and 0b111
            val cond = regA
            val aluOp = code and 0b111111
            val value = code and 0b11111111

            fun getReg(n: Int) = WritableRegister.entries.find { it.code == n } ?: error("Bad register code")

            fun getXReg(): Register =
                if (xReg and 0b1000 == 0) getReg(xReg)
                else ReadOnlyRegister.entries.find { it.xCode == xReg } ?: error("Bad readonly register code")

            return when (op) {
                0b000 -> HLT
                0b001 -> NOP
                0b010 -> MOV(getXReg(), getReg(regB))
                0b011 -> SET(flag, getReg(regA), value)
                0b100 -> JMP(JumpCondition(cond), getReg(regB))
                0b101 -> ALU(
                    flag, getReg(regA), getReg(regB),
                    AluOperation.entries.find { it.opcode == aluOp } ?: error("Bad ALU code")
                )
                0b110 -> MEM(flag, getReg(regA), getReg(regB))
                0b111 -> IO(flag, getReg(regA), getReg(regB))
                else -> error("Bad opcode")
            }
        }
    }

    val code = (opcode shl 13) or if (masks.isNotEmpty()) masks.reduce(Int::or) else 0

    data object HLT : Instruction(0b000)
    data object NOP : Instruction(0b001)

    data class MOV(val src: Register, val dest: WritableRegister) :
        Instruction(0b010, src.xCode shl 9, dest.code shl 6)

    data class SET(val sideFlag: Boolean, val dest: WritableRegister, val value: Int) :
        Instruction(0b011, boolMask(sideFlag), dest.code shl 9, value)

    data class JMP(val cond: JumpCondition, val addr: WritableRegister) :
        Instruction(0b100, cond.mask, addr.code shl 6)

    data class ALU(val q: Boolean, val a: WritableRegister, val b: WritableRegister, val op: AluOperation) :
        Instruction(0b101, boolMask(q), a.code shl 9, b.code shl 6, op.mask)

    data class MEM(val w: Boolean, val addr: WritableRegister, val d: WritableRegister) :
        Instruction(0b110, boolMask(w), addr.code shl 9, d.code shl 6)

    data class IO(val w: Boolean, val addr: WritableRegister, val d: WritableRegister) :
        Instruction(0b111, boolMask(w), addr.code shl 9, d.code shl 6)
}
