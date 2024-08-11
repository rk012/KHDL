package assembler

import VirtualMachine
import assembler.instructions.*
import common.AluOperation
import common.WritableRegister
import common.testing.IOListener
import kotlin.test.Test
import kotlin.test.assertEquals

class StackTest {
    private fun createVm(asm: Assembly) = VirtualMachine(link(null, assemble(AsmConfig(), asm)).bytecode)

    @Test
    fun pushPop() {
        val asm = asm {
            +set(WritableRegister.SP, 0)

            +push(12)
            +push(34)
            +push(-6093)
            +push(0)

            +pop(WritableRegister.A)
            +pop(WritableRegister.B)
            +pop(WritableRegister.C)
            +pop(WritableRegister.D)

            +hlt()
        }

        val vm = createVm(asm)
        vm.runUntilHalt()

        assertEquals(0, vm.debugRegister(WritableRegister.A))
        assertEquals(-6093, vm.debugRegister(WritableRegister.B))
        assertEquals(34, vm.debugRegister(WritableRegister.C))
        assertEquals(12, vm.debugRegister(WritableRegister.D))
    }

    @Test
    fun call() {
        val asm = asm {
            +set(WritableRegister.SP, 0)
            +mov(WritableRegister.SP to WritableRegister.BP)
            +push(14)
            +push(28)
            +callLocal("add")
            +mov(WritableRegister.A to WritableRegister.C)
            +hlt()

            +"add"
            +lea(-2)
            +memRead(WritableRegister.P, WritableRegister.B)
            +lea(-3)
            +memRead(WritableRegister.P, WritableRegister.A)
            +aluP(WritableRegister.A, WritableRegister.B, AluOperation.A_PLUS_B)
            +mov(WritableRegister.P, WritableRegister.A)
            +ret()
        }

        val vm = createVm(asm)
        vm.runUntilHalt()

        assertEquals(42, vm.debugRegister(WritableRegister.C))
    }

    @Test
    fun helloWorld() {
        val str = "Hello World!"
        val asm = asm {
            +set(WritableRegister.SP, 0)
            +mov(WritableRegister.SP, WritableRegister.BP)

            +localRef("str_data")
            +mov(WritableRegister.P to WritableRegister.A)

            +"loop"
            +memRead(WritableRegister.A, WritableRegister.B)
            +localRef("loop_end")
            +aluQ(WritableRegister.B, WritableRegister.B, AluOperation.A)
            +je(WritableRegister.P) // while b != 0 <=> jmp if b==0

            +push(WritableRegister.A)  // saved
            +push(WritableRegister.B)  // char arg
            +callLocal("print")
            +pop(WritableRegister.Q) // unused arg
            +pop(WritableRegister.A) // load saved addr

            +aluP(WritableRegister.A, WritableRegister.A, AluOperation.A_PLUS_1)
            +mov(WritableRegister.P to WritableRegister.A) // a++

            +localRef("loop")
            +jmp(WritableRegister.P)

            +"loop_end"
            +hlt()

            +"str_data"
            +rawData(str.map { it.code.toShort() } + 0x00)

            +"print"
            +set(WritableRegister.A, 0)
            +lea(-2)
            +memRead(WritableRegister.P, WritableRegister.B)
            +ioWrite(WritableRegister.A, WritableRegister.B)
            // Print 0x00 to ensure IOListener reads duplicate consecutive chars
            +ioWrite(WritableRegister.A, WritableRegister.A)
            +ret()
        }

        val vm = createVm(asm)
        val dev = vm.ioController.install(::IOListener)

        vm.runUntilHalt()

        assertEquals(
            "Hello World!",
            dev.output.joinToString("") { it.toInt().toChar().toString() }.filter { it.code != 0 }
        )
    }
}