package hdl

import kotlin.random.Random

class Clock {
    private val chips = mutableListOf<DFF>()

    var nonce = Random.nextInt()
        private set

    internal fun addChip(chip: DFF) {
        chips.add(chip)
    }

    fun pulse() {
        chips.forEach { it.tick(nonce) }
        chips.forEach { it.tock() }

        nonce++
    }
}