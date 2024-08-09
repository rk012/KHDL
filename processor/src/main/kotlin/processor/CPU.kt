@file:Suppress("PrivatePropertyName")

package processor

import common.CpuInstruction.NOP.code as NOP_CODE
import hardware.*
import hdl.*
import processor.components.*
import processor.components.CpuRegisters
import processor.components.InstructionDecoder
import processor.components.JmpCmp
import processor.components.ProgramCounter
import processor.components.XReg

internal class CPU(clk: Clock) {
    // Inputs
    private val enableH = PinHeader(1)
    private val memDataH = PinHeader(16)
    private val ioDataH = PinHeader(16)
    private val dbgModeH = PinHeader(2)
    private val dbgInH = PinHeader(16)

    // Major components
    private val fetchState = FetchState(clk)
    private val decoder = InstructionDecoder()

    private val pc = ProgramCounter(clk, 16)
    private val nextInstruction = Register(clk, 16)
    private val registers = CpuRegisters(clk)
    private val flags = Register(clk, 16)
    private val xReg = XReg()

    private val cmp = JmpCmp()
    private val alu = ALU(16)

    private val setPartialWrite = PartialWrite(16)

    // Fetch/Exec pin
    private val notFetch = Not()
    private val exec = notFetch.out
    private val fetch = fetchState.isFetch

    init {
        notFetch.a bind fetch
    }

    // Direct connections
    init {
        fetchState.enable bind enableH.output[0]

        nextInstruction.d bind memDataH.output
        nextInstruction.w bind fetch

        listOf(alu.za, alu.na, alu.zb, alu.nb, alu.f, alu.no) bind decoder.aluOp
        alu.a bind registers.a
        alu.b bind registers.b

        flags.d bind (List(13) { PinSource(false) } + listOf(alu.zero, alu.neg, alu.overflow))

        // pc disabled if using dbg instruction
        // pc input 0 if not enabled, write true
        // pc.input bind registers.b

        xReg.cpuRegOut bind registers.a
        xReg.ipRegOut bind pc.out
        xReg.flagsRegOut bind flags.out
        // xAddr dbg injection

        cmp.cond bind decoder.cond
        cmp.eq bind flags.out[13]
        cmp.neg bind flags.out[14]
        cmp.overflow bind flags.out[15]

        // addr A w/ dbg injection is specified later
        registers.addrB bind decoder.regB

        setPartialWrite.partial bind decoder.iVal
        setPartialWrite.sideFlag bind decoder.ab
        setPartialWrite.oldVal bind registers.a
    }

    // DBG modes
    private val dMux_dbgMode = DMux(2)
    private val dbgReg = dMux_dbgMode.out[1]
    private val dbgMem = dMux_dbgMode.out[2]
    private val dbgInstruction = dMux_dbgMode.out[3]

    init {
        dMux_dbgMode.addr bind dbgModeH.output
        dMux_dbgMode.input bind PinSource(true)
    }

    // FLAGS write
    private val and_alu_exec = And()

    init {
        and_alu_exec.a bind decoder.alu
        and_alu_exec.b bind exec
        flags.w bind and_alu_exec.out
    }

    // PC enable
    private val notDbgInstruction = Not()
    private val and_fetch_notDbgInstruction = And()

    init {
        notDbgInstruction.a bind dbgInstruction
        and_fetch_notDbgInstruction.a bind fetch
        and_fetch_notDbgInstruction.b bind notDbgInstruction.out
        pc.en bind and_fetch_notDbgInstruction.out
    }

    // CPU Reg A
    private val dbgRegAddrASwitch = BusSwitch(3)

    init {
        dbgRegAddrASwitch.a bind decoder.regA
        dbgRegAddrASwitch.b bind dbgInH.output.subList(13, 16)
        dbgRegAddrASwitch.select bind dbgReg
        registers.addrA bind dbgRegAddrASwitch.out
    }

    // PC Input
    private val pcInputSwitch = BusSwitch(16)

    init {
        pcInputSwitch.a bind BusSource(16).apply { setN(0) }.outputBus
        pcInputSwitch.b bind registers.b
        pcInputSwitch.select bind enableH.output[0]

        pc.input bind pcInputSwitch.out
    }

    // CMP PC JMP
    private val notEn = Not()

    private val and_cmp_jmpEn = And()
    private val and_cmpOp_exec = And()
    private val and_cmpJmpEn_cmpOpExec = And()
    private val or_jmp_notEn = Or()

    init {
        notEn.a bind enableH.output[0]

        and_cmp_jmpEn.a bind decoder.ab
        and_cmp_jmpEn.b bind cmp.output
        and_cmpOp_exec.a bind decoder.cmp
        and_cmpOp_exec.b bind exec

        and_cmpJmpEn_cmpOpExec.a bind and_cmp_jmpEn.out
        and_cmpJmpEn_cmpOpExec.b bind and_cmpOp_exec.out

        or_jmp_notEn.a bind and_cmpJmpEn_cmpOpExec.out
        or_jmp_notEn.b bind notEn.out

        pc.w bind or_jmp_notEn.out
    }

    // xReg addr
    private val xRegAddrSwitch = BusSwitch(4)

    init {
        xRegAddrSwitch.a bind decoder.xReg
        xRegAddrSwitch.b bind dbgInH.output.subList(12, 16)
        xRegAddrSwitch.select bind dbgReg
        xReg.xAddr bind xRegAddrSwitch.out
    }

    // Instruction line
    private val dbgInstrSw = BusSwitch(16)
    private val nopSw = BusSwitch(16)

    init {
        dbgInstrSw.a bind nextInstruction.out
        dbgInstrSw.b bind dbgInH.output
        dbgInstrSw.select bind dbgInstruction

        nopSw.a bind dbgInstrSw.out
        nopSw.b bind BusSource(16).apply { setN(NOP_CODE.toInt()) }.outputBus
        nopSw.select bind notEn.out

        decoder.instruction bind nopSw.out
    }

    // Register Write Input Data line
    private val partialSwitch = BusSwitch(16)
    private val aluOutSwitch = BusSwitch(16)
    private val memDataSwitch = BusSwitch(16)
    private val ioDataSwitch = BusSwitch(16)
    private val cmpDataSwitch = BusSwitch(16)

    init {
        partialSwitch.a bind xReg.xRegOut
        partialSwitch.b bind setPartialWrite.newVal
        partialSwitch.select bind decoder.set

        aluOutSwitch.a bind partialSwitch.out
        aluOutSwitch.b bind alu.out
        aluOutSwitch.select bind decoder.alu

        memDataSwitch.a bind aluOutSwitch.out
        memDataSwitch.b bind memDataH.output
        memDataSwitch.select bind decoder.mem

        ioDataSwitch.a bind memDataSwitch.out
        ioDataSwitch.b bind ioDataH.output
        ioDataSwitch.select bind decoder.io

        cmpDataSwitch.a bind ioDataSwitch.out
        cmpDataSwitch.b bind List(15) { PinSource(false) } + listOf(cmp.output)
        cmpDataSwitch.select bind decoder.cmp

        registers.d bind cmpDataSwitch.out
    }

    // Data Output Line direct regB -> dataOut

    // Address Output Line
    private val pcOutSwitch = BusSwitch(16)
    private val dbgMemSwitch = BusSwitch(16)

    init {
        pcOutSwitch.a bind registers.a
        pcOutSwitch.b bind pc.out
        pcOutSwitch.select bind fetch

        dbgMemSwitch.a bind pcOutSwitch.out
        dbgMemSwitch.b bind dbgInH.output
        dbgMemSwitch.select bind dbgMem

        // addrOut
    }

    // Reg Write Select Line
    private val selectRegASwitch = BusSwitch(3)
    private val selectAluOutAddr = BusSwitch(3)

    init {
        selectRegASwitch.a bind decoder.regB
        selectRegASwitch.b bind decoder.regA
        selectRegASwitch.select bind decoder.set

        selectAluOutAddr.a bind selectRegASwitch.out
        selectAluOutAddr.b bind listOf(PinSource(true), PinSource(false), decoder.ab)
        selectAluOutAddr.select bind decoder.alu

        registers.wAddr bind selectAluOutAddr.out
    }

    // Reg Write Enable

    private class RegWriteEnable {
        private val nopOrHlt = Or()
        private val memOrIO = Or()
        private val memIoOrCmp = Or()
        private val memIoCmpAndAB = And()
        private val bigOr = Or()
        private val nor = Nor()

        init {
            nopOrHlt.out bind bigOr.a
            memOrIO.out bind memIoOrCmp.b
            memIoOrCmp.out bind memIoCmpAndAB.b
            memIoCmpAndAB.out bind bigOr.b
            bigOr.out bind nor.b
        }

        val nop = nopOrHlt.a
        val hlt = nopOrHlt.b
        val ab = memIoCmpAndAB.a
        val mem = memOrIO.a
        val io = memOrIO.b
        val cmp = memIoOrCmp.a
        val fetch = nor.a

        val write = nor.out
    }

    private val regWriteEnable = RegWriteEnable()

    init {
        regWriteEnable.nop bind decoder.nop
        regWriteEnable.hlt bind decoder.hlt
        regWriteEnable.ab bind decoder.ab
        regWriteEnable.mem bind decoder.mem
        regWriteEnable.io bind decoder.io
        regWriteEnable.cmp bind decoder.cmp
        regWriteEnable.fetch bind fetch

        registers.w bind regWriteEnable.write
    }

    // Dbg out

    private val dbgOutSwitch = BusSwitch(16)

    init {
        dbgOutSwitch.a bind xReg.xRegOut
        dbgOutSwitch.b bind memDataH.output
        dbgOutSwitch.select bind dbgMem

        // dbgOut
    }


    // Mem, IO write

    private val and_ab_exec = And()
    private val w_io_and = And()
    private val w_mem_and = And()

    init {
        and_ab_exec.a bind decoder.ab
        and_ab_exec.b bind exec

        w_io_and.a bind decoder.io
        w_io_and.b bind and_ab_exec.out

        w_mem_and.a bind decoder.mem
        w_mem_and.b bind and_ab_exec.out

        // mem write
        // io write
    }

    // HLT

    private val hltAndExec = And()

    init {
        hltAndExec.a bind decoder.hlt
        hltAndExec.b bind exec
    }

    // Instruction Status
    private val and_exec_en = And()

    init {
        and_exec_en.a bind exec
        and_exec_en.b bind enableH.output[0]
    }

    // Inputs
    val enable = enableH.input[0]

    val memData: InputBus = memDataH.input
    val ioData: InputBus = ioDataH.input

    val dbgMode: InputBus = dbgModeH.input
    val dbgIn: InputBus = dbgInH.input

    // Outputs
    val hlt = hltAndExec.out

    val dataOut: OutputBus = registers.b
    val addrOut: OutputBus = dbgMemSwitch.out

    val memWrite = w_mem_and.out
    val ioWrite = w_io_and.out

    val dbgOut: OutputBus = dbgOutSwitch.out

    val execActive = and_exec_en.out
}