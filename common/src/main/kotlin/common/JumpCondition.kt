package common

data class JumpCondition(val eq: Boolean, val lt: Boolean, val gt: Boolean) {
    constructor(code: Int) : this((code and 0b100) != 0, (code and 0b010) != 0, (code and 0b001) != 0)

    val mask = listOf(eq, lt, gt).fold(0) { acc, b -> (acc shl 1) or if (b) 1 else 0 } shl 9

    fun shouldJump(n: Int) = eq && n == 0 || lt && n < 0 || gt && n > 0
}
