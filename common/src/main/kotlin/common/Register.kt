package common

sealed interface Register {
    val code: Int
    val xCode: Int
}

enum class WritableRegister(override val code: Int) : Register {
    A   (0b000),
    B   (0b001),
    C   (0b010),
    D   (0b011),
    P   (0b100),
    Q   (0b101),
    BP  (0b110),
    SP  (0b111);

    override val xCode = 0b0000 or code
}

enum class ReadOnlyRegister(override val code: Int) : Register {
    IP      (0b000),
    FLAGS   (0b001);

    override val xCode = 0b1000 or code
}
