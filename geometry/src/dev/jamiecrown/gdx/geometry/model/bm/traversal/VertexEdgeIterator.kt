package dev.jamiecrown.gdx.geometry.model.bm.traversal

import dev.jamiecrown.gdx.geometry.model.bm.struct.BMEdge
import dev.jamiecrown.gdx.geometry.model.bm.struct.BMVertex


class VertexEdgeIterator(val vertex: BMVertex, val edge: BMEdge?) : Iterator<BMEdge> {

    private var current: BMEdge? = null
    private var first = false

    init {
        current = edge
        first = (current != null)
    }

    override fun hasNext(): Boolean {
        return (current !== edge) || first
    }

    override fun next(): BMEdge {
        first = false
        val edge = current
        current = current?.getNextEdge(vertex)
        return edge!!
    }

}