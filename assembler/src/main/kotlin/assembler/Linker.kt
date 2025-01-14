package assembler

import common.*

data class ObjectFile(
    val imports: List<String>,
    val exports: Map<String, UShort>,
    val bytecode: Bytecode
) {
    val size = imports.size + bytecode.size
}


data class Executable(
    val bytecode: Bytecode,
    val callTable: Map<String, UShort>
)


fun link(entryPoint: String?, vararg objs: ObjectFile): Executable {
    val offsets = (objs zip objs.runningFold(0) { acc, obj -> acc + obj.size }).toMap()
    val callTable = mutableMapOf<String, UShort>()

    objs.forEach { obj ->
        val offset = offsets[obj]!!

        obj.exports.forEach { (name, pos) ->
            require(name !in callTable) { "Duplicate export: $name" }

            val fullOffset = offset + obj.imports.size + pos.toInt()

            require(fullOffset in 0..UShort.MAX_VALUE.toInt()) { "Binary too large" }

            callTable[name] = fullOffset.toUShort()
        }
    }

    val bytecode = objs.flatMap { obj ->
        obj.imports.mapIndexed { i, s ->
            val dest = requireNotNull(callTable[s]) { "Undefined import: $s" }.toInt()
            val pos = offsets[obj]!! + i

            val diff = dest - pos
            require(diff in Short.MIN_VALUE..Short.MAX_VALUE) { "Binary too large" }

            diff.toShort()
        } + obj.bytecode
    }

    val entryPointOffset = if (entryPoint != null) {
        requireNotNull(callTable[entryPoint]) { "Undefined entry point: $entryPoint" }.toInt()
    } else {
        0
    }

    val startOffset = entryPointOffset + 2 // IP + 2 -> bytecode start

    val initInstructions = if (entryPointOffset != 0) listOf(
        CpuInstruction.SET(true, WritableRegister.P, (startOffset shr 8) and 0xFF),
        CpuInstruction.SET(false, WritableRegister.P, startOffset and 0xFF),
        CpuInstruction.MOV(ReadOnlyRegister.IP, WritableRegister.Q),
        CpuInstruction.ALU(false, WritableRegister.P, WritableRegister.Q, AluOperation.A_PLUS_B),  // <- IP
        CpuInstruction.CMP(true, JumpCondition(0b111), WritableRegister.P)
    ).map(CpuInstruction::code) else emptyList()

    return Executable(
        initInstructions + bytecode,
        callTable.mapValues { (it.value.toInt() + initInstructions.size).toUShort() }
    )
}
