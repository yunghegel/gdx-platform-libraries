package dev.jamiecrown.gdx.core.util

class Mask {

    private var mask: Int = 0

    fun set(bit: Int, value: Boolean) {
        if (value) {
            mask = mask or (1 shl bit)
        } else {
            mask = mask and (1 shl bit).inv()
        }
    }

    fun get(bit: Int): Boolean {
        return mask and (1 shl bit) != 0
    }

    fun clear() {
        mask = 0
    }

    fun copy(): Mask {
        val mask = Mask()
        mask.mask = this.mask
        return mask
    }

}