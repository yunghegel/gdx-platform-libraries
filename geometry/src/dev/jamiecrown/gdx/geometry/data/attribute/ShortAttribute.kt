package dev.jamiecrown.gdx.geometry.data.attribute

import dev.jamiecrown.gdx.geometry.core.Indexed

class ShortAttribute<E : Indexed>(name: String) : AbstractProperty<E, Short, ShortArray>(1, name) {

    override fun get(element: E): Short {
        return data!![indexOf(element)]
    }

    override fun set(element: E, value: Short) {
        data!![indexOf(element)] = value
    }

    override fun equals(a: E, b: E): Boolean {
        val aIndex = indexOf(a)
        val bIndex = indexOf(b)
        return data!![aIndex] == data!![bIndex]
    }

    override fun alloc(size: Int): ShortArray {
        return ShortArray(size) { 0 }
    }
}