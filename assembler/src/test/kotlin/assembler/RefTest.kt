package assembler

import VirtualMachine
import assembler.instructions.*
import common.AluOperation
import common.WritableRegister
import common.testing.IOListener
import s
import kotlin.test.Test
import kotlin.test.assertEquals

class RefTest {
    @Test
    fun fibs() {
        val asm = asm {
            +set(WritableRegister.A, 1)
            +set(WritableRegister.B, 0)
            +mov(WritableRegister.B to WritableRegister.C)

            +"loop"
            +ioWrite(WritableRegister.C, WritableRegister.A)
            +aluP(WritableRegister.A, WritableRegister.B, AluOperation.A_PLUS_B)
            +mov(WritableRegister.A to WritableRegister.B)
            +mov(WritableRegister.P to WritableRegister.A)
            +localRef("loop")
            +aluQ(WritableRegister.A, WritableRegister.B, AluOperation.A_MINUS_B)
            +jge(WritableRegister.P)
            +hlt()
        }

        val vm = VirtualMachine(link(null, assemble(AsmConfig(), asm)).bytecode)

        val expected: List<Short> = sequence {
            var a = 1
            var b = 0

            while (true) {
                yield(a.s)
                a = (a+b).also { b = a }
            }
        }.drop(1).takeWhile { it > 0 }.toList()

        val dev = vm.ioController.install(::IOListener)

        vm.runUntilHalt()

        assertEquals(expected, dev.output)
    }

    @Test
    fun imported() {
        val exported = asm {
            +"data_lbl"
            +rawData(listOf(42))
        }.let { assemble(AsmConfig(directAddresses = false, exportLabels = mapOf("exported" to "data_lbl")), it) }

        val main = asm {
            +"main"
            +importedRef("exported")
            +memRead(WritableRegister.P, WritableRegister.A)
            +hlt()
        }.let {
            assemble(
                AsmConfig(directAddresses = false, imports = listOf("exported"), exportLabels = mapOf("main" to "main")),
                it
            )
        }

        val vm = VirtualMachine(link("main", exported, main).bytecode)
        vm.runUntilHalt()
        assertEquals(42, vm.debugRegister(WritableRegister.A))
    }
}