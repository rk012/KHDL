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

class TestBus(private val outputBus: OutputBus) {
    fun test(
        inputs: List<Int>,
        expected: List<Int?>,
        clk: Clock,
        inputBus: BusSource
    ) {
        require(inputs.size == expected.size)

        (inputs zip expected).forEachIndexed { i, (input, expect) ->
            inputBus.setN(input)

            require(expect == null || this.outputBus.peekInt(clk.nonce) == expect) {
                """
                    |Expected ${expect!!.toString(2)}
                    |Output: ${this.outputBus.peekInt(clk.nonce).toString(2)}
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
        inputs.map { it shr this.outputBus.size },
        inputs.map { it and ((1 shl this.outputBus.size) - 1)},
        clk,
        src
    )
}