package compiler

import VirtualMachine
import assembler.AsmConfig
import assembler.asm
import assembler.assemble
import assembler.instructions.callLocal
import assembler.instructions.hlt
import assembler.instructions.mov
import assembler.instructions.set
import assembler.link
import common.WritableRegister
import kotlin.test.Test
import kotlin.test.assertEquals

class EvalTests {
    private val cases = mapOf(
        "1 + 2" to 3,
        "10 - 3" to 7,
        "5 << 1" to 10,
        "16 >> 2" to 4,
        "~1" to -2,
        "-10 + 5" to -5,
        "7 & 3" to 3,
        "6 | 1" to 7,
        "4 ^ 2" to 6,
        "10 - (2 + 3)" to 5,
        "(~6 & 3) | 4" to 5,
        "5 ^ (3 | -1)" to -6,
        "(8 >> 2) + (4 << 1)" to 10,
        "~(7 & 3)" to -4,
        "(15 >> 1) | (1 << 3)" to 15,
        "(2 ^ -3) + (4 & 5)" to 3,
        "-10 & (8 >> 2)" to 2,
        "(6 | 2) ^ 3" to 5,
        "(1 + 3) << 2" to 16,
        "((5 & 7) << 2) | 1" to 21,
        "1 && 1" to 1,
        "-1 && 0" to 0,
        "0 || 0" to 0,
        "1 && 0" to 0,

        // TODO Test short circuit logic after adding assignment, local vars
    )

    @Test
    fun evalTests() {
        cases.forEach { (expr, res) ->
            val src = """
                int main() {
                    return $expr;
                }
            """.trimIndent()
            val compiled = compileSource(src)
            val executable = asm {
                +set(WritableRegister.SP, 0)
                +mov(WritableRegister.SP to WritableRegister.BP)
                +callLocal("__fn_main")
                +hlt()

                addAll(compiled)
            }.let {
                link(null, assemble(AsmConfig(), it))
            }

            val vm = VirtualMachine(executable.bytecode)
            vm.runUntilHalt()
            assertEquals(res.toShort(), vm.debugRegister(WritableRegister.A), "Expr: $expr")
        }
    }
}