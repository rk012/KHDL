package hdl

class Clock {
    private val chips = mutableListOf<ClockedChip>()

    var nonce = Any()

    @InternalHdlApi
    fun addChip(chip: ClockedChip) {
        chips.add(chip)
    }

    fun pulse() {
        chips.forEach { it.tick(nonce) }
        chips.forEach { it.tock() }

        nonce = Any()
    }
}