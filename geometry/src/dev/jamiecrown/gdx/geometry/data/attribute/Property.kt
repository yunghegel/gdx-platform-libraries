package dev.jamiecrown.gdx.geometry.data.attribute

import dev.jamiecrown.gdx.geometry.core.Indexed

interface Property<O : Indexed, T, TArr> {


    var data: TArr?

    operator fun get(index: O): T?

    val name: String

    val numComponents: Int

    operator fun set(index: O, value: T)

    fun alloc(size: Int): TArr

    fun equals(a: O, b: O): Boolean

    fun indexOf(element: O, position: Int): Int {
        require(position < numComponents)
        return (element.index * numComponents) + position
    }

    fun indexOf(element: O): Int {
        return element.index * numComponents
    }

    fun copy(a: O, b: O) {
        val aIndex = indexOf(a)
        val bIndex = indexOf(b)
        System.arraycopy(data, aIndex, data, bIndex, numComponents)
    }

    fun <O2 : O> copy(from: O, to: O, other: Property<O, T, TArr>) {
        require(numComponents == other.numComponents) { "Cannot copy between attributes with different number of components" }
        val fromIndex = indexOf(from)
        val toIndex = other.indexOf(to)
        System.arraycopy(data, fromIndex, other.data, toIndex, numComponents)
    }


    fun allocReplace(size: Int): TArr? {
        val oldArr = data
        data = alloc(size * numComponents)
        return oldArr
    }

    fun realloc(size: Int, copyLength: Int) {
        val oldArr = allocReplace(size)
        System.arraycopy(oldArr!!, 0, data, 0, copyLength * numComponents)
    }

}