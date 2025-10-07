package dev.jamiecrown.gdx.geometry.model.he.struct

import dev.jamiecrown.gdx.geometry.core.Edge

class HEdge() : Edge() {

    override val kind: Int = HE_VERTEX

    lateinit var vertex: HVertex
    lateinit var face: HFace
    var next: HEdge? = null
    var pair: HEdge? = null

    override fun release() {
        vertex = HVertex()
        face = HFace()
        next = null
        pair = null

    }

}