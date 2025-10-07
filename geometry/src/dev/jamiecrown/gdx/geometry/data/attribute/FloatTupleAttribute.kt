package dev.jamiecrown.gdx.geometry.data.attribute

import dev.jamiecrown.gdx.core.util.EPSILON
import dev.jamiecrown.gdx.geometry.core.Indexed
import kotlin.math.abs

class FloatTupleAttribute<E : Indexed>(name: String, components: Int) :
    AbstractProperty<E, FloatArray, FloatArray>(components, name) {

    override fun get(element: E): FloatArray {
        val index = indexOf(element)
        return FloatArray(numComponents) { data!![index + it] }
    }

    override fun set(element: E, value: FloatArray) {
        val index = indexOf(element)
        for (i in 0 until numComponents) {
            data!![index + i] = value[i]
        }
    }




    override fun equals(a: E, b: E): Boolean {
        val aIndex = indexOf(a)
        val bIndex = indexOf(b)
        for (i in 0 until numComponents) {
            if (abs(data!![aIndex + i] - data!![bIndex + i]) > EPSILON) {
                return false
            }
        }
        return true
    }

    override fun alloc(size: Int): FloatArray {
        return FloatArray(size)
    }

}