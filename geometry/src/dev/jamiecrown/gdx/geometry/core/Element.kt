package dev.jamiecrown.gdx.geometry.core

import dev.jamiecrown.gdx.geometry.core.Indexed.Companion.FLAG_INDEX_MODIFIED
import dev.jamiecrown.gdx.core.util.Mask

abstract class Element() : Indexed {

    override var index: Int = -1
        set (value) {
            val old = field
            field = value
            if (old != -1) {
                flags.set(FLAG_INDEX_MODIFIED, true)
            }
        }

    val initialized : Boolean
        get() = index != -1

    constructor(index: Int) : this() { this.index = index }


    abstract val kind : Int

    override val flags = Mask()


    override fun reset() {
        release()
        index = -1
        flags.clear()
    }

    companion object {

        const val BM_VERTEX: Int = 0
        const val BM_EDGE: Int = 1
        const val BM_FACE: Int = 2
        const val BM_LOOP: Int = 4

        const val HE_VERTEX: Int = 0
        const val HE_HALFEDGE: Int = 1
        const val HE_FACE: Int = 2

        const val IFS_VERTEX: Int = 0
        const val IFS_EDGE: Int = 1
        const val IFS_FACE: Int = 2
    }

}