package dev.jamiecrown.gdx.geometry.data.attribute

import dev.jamiecrown.gdx.core.util.EPSILON
import dev.jamiecrown.gdx.geometry.core.Indexed
import kotlin.math.abs

class ShortTupleAttribute<E : Indexed>(name: String, numComponents: Int) :
    AbstractProperty<E, ShortArray, Array<ShortArray>>(numComponents, name) {

    override fun get(element: E): ShortArray {
        val index = indexOf(element)
        return data?.get(index) ?: ShortArray(numComponents) { 0 }

    }

    override fun set(element: E, value: ShortArray) {
        val index = indexOf(element)
        data!![index] = value
    }

    override fun equals(a: E, b: E): Boolean {
        val aIndex = indexOf(a)
        val bIndex = indexOf(b)
        for (i in 0 until numComponents) {
            if (abs(data!![aIndex][i] - data!![bIndex][i]) > EPSILON) {
                return false
            }
        }
        return true
    }

    override fun alloc(size: Int): Array<ShortArray> {
        return Array(size) { ShortArray(numComponents) { 0 } }
    }
}