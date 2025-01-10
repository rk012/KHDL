package compiler

import VirtualMachine
import common.WritableRegister
import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalFunctions {
    val fooSrc = """
        int foo(int a, int b) {
            return a + b;
        }
    """.trimIndent()

    val mainSrc = """
        int foo(int a, int b);
        
        int main() {
            return foo(1, 2*2);
        }
    """.trimIndent()

    @Test
    fun externalFunctionTest() {
        val exe = compileExe(
            CRuntime.CRT0,
            compileObj(fooSrc),
            compileObj(mainSrc)
        )

        val vm = VirtualMachine(exe.bytecode)
        vm.runUntilHalt()
        assertEquals(5, vm.debugRegister(WritableRegister.A))
    }
}