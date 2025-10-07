package dev.jamiecrown.gdx.geometry.model.bm.traversal

import dev.jamiecrown.gdx.geometry.model.bm.struct.BMLoop

class EdgeLoopIterator(val loop: BMLoop?) : Iterator<BMLoop> {
    private val startLoop = loop
    private var currentLoop = loop
    private var first = (loop != null)

    override fun hasNext(): Boolean {
        return currentLoop != startLoop || first
    }

    override fun next(): BMLoop {
        first = false
        val loop = currentLoop
        currentLoop = currentLoop?.nextEdgeLoop
        return loop ?: throw NoSuchElementException()
    }
}