package hdl

import kotlin.random.Random

class Clock {
    private val chips = mutableListOf<ClockedChip>()

    var nonce = Random.nextInt()
        private set

    @InternalHdlApi
    fun addChip(chip: ClockedChip) {
        chips.add(chip)
    }

    fun pulse() {
        chips.forEach { it.tick(nonce) }
        chips.forEach { it.tock() }

        nonce++
    }
}