package hdl

class TestPin(private val src: OutputPin) : OutputPin by src {
    fun test(
        inputs: List<Boolean>,
        expected: List<Boolean?>,
        clk: Clock,
        src: PinSource
    ) {
        require(inputs.size == expected.size)

        (inputs zip expected).forEach { (input, expect) ->
            src.value = input

            require(expect == null || peek(clk.nonce) == expect)

            clk.pulse()
        }
    }
}

class TestBus(private val src: OutputBus) {
    fun peekInt(nonce: Int? = null) = src.map { it.peek(nonce) }.fold(0) { acc, bit -> (acc shl 1) or if (bit) 1 else 0 }

    fun test(
        inputs: List<Int>,
        expected: List<Int?>,
        clk: Clock,
        src: BusSource
    ) {
        require(inputs.size == expected.size)

        (inputs zip expected).forEachIndexed { i, (input, expect) ->
            src.setN(input)

            require(expect == null || peekInt(clk.nonce) == expect) {
                """
                    |Expected ${expect!!.toString(2)}
                    |Output: ${peekInt(clk.nonce).toString(2)}
                    |Input: ${input.toString(2)}
                    |Index: $i
                """.trimMargin()
            }

            clk.pulse()
        }
    }

    fun testLines(
        inputs: List<Int>,
        clk: Clock,
        src: BusSource
    ) = test(
        inputs.map { it shr this.src.size },
        inputs.map { it and ((1 shl this.src.size) - 1)},
        clk,
        src
    )
}