package dev.jamiecrown.gdx.geometry.model.he.struct

import dev.jamiecrown.gdx.geometry.core.Element
import dev.jamiecrown.gdx.geometry.core.Element.Companion.HE_FACE
import dev.jamiecrown.gdx.geometry.core.Face

class HFace : Face() {

    override val kind: Int = HE_FACE

    var halfEdge: HEdge? = null

    override fun release() {
        halfEdge = null
    }

}