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

class LocalVars {
    private val source = """
        int main() {
            int a = 2+1;
            int b;
            b = a << 1;
            int c = a = b;
            return c + (a - b);
        }
    """.trimIndent()

    @Test
    fun localVarTest() {
        val compiled = compileSource(source)

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
        assertEquals(6, vm.debugRegister(WritableRegister.A))
    }
}