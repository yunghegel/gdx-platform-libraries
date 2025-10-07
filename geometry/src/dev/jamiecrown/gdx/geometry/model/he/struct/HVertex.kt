package dev.jamiecrown.gdx.geometry.model.he.struct

import dev.jamiecrown.gdx.geometry.core.Vertex

class HVertex() : Vertex(){

    override val kind: Int = HE_VERTEX

    var halfEdge: HEdge? = null

    override fun release() {
        halfEdge = null
    }



}