package dev.jamiecrown.gdx.geometry.model.bm.traversal

import dev.jamiecrown.gdx.geometry.model.bm.struct.BMLoop

class LoopMapIterator<E>(private val it: Iterator<BMLoop>, private val mapFunc: (BMLoop)->E) :
    MutableIterator<E> {


    override fun hasNext(): Boolean {
        return it.hasNext()
    }

    override fun next(): E {
        return mapFunc(it.next())
    }

    override fun remove() {

    }
}