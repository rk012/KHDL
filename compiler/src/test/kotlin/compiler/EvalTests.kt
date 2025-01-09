package compiler

import VirtualMachine
import common.WritableRegister
import kotlin.test.Test
import kotlin.test.assertEquals

class EvalTests {
    private val cases = mapOf(
        "1 + 2" to 3,
        "-5" to -5,
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
        "0 ? 1+1 : 2+3" to 5,
        "5*6" to 30,
        "-2*-3" to 6,
        "500*-40" to -20000,
        "-8192*4" to -32768,
        "10/3" to 3,
        "30001/4" to 7500,
        "3/-32768" to 0,
        "-32768/-32768" to 1,
        "-32768/4" to -8192,
        "10%3" to 1,
        "-10%3" to -1,
    )

    @Test
    fun evalTests() {
        cases.forEach { (expr, res) ->
            val src = """
                int main() {
                    return $expr;
                }
            """.trimIndent()

            val vm = VirtualMachine(compileSingleSource(src).bytecode)
            vm.runUntilHalt()
            assertEquals(res.toShort(), vm.debugRegister(WritableRegister.A), "Expr: $expr")
        }
    }
}