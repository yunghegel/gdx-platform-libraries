package dev.jamiecrown.gdx.geometry.model.bm.traversal

import dev.jamiecrown.gdx.geometry.model.bm.struct.BMLoop

class FaceLoopIterator(loop: BMLoop) : MutableIterator<BMLoop> {
    private val startLoop: BMLoop
    private var currentLoop: BMLoop
    private var first = true

    init {
        startLoop = loop
        currentLoop = loop
    }

    override fun hasNext(): Boolean {
        return currentLoop !== startLoop || first
    }

    override fun next(): BMLoop {
        first = false
        val loop: BMLoop = currentLoop
        currentLoop = currentLoop.nextFaceLoop!!
        return loop
    }

    override fun remove() {
        throw UnsupportedOperationException()
    }
}