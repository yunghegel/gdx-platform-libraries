package dev.jamiecrown.gdx.geometry.data.attribute

import com.badlogic.gdx.math.Vector2
import dev.jamiecrown.gdx.geometry.core.Indexed

class Vector2Attribute<E : Indexed>(name: String) : AbstractProperty<E, Vector2, FloatArray>(2, name) {

    override fun get(element: E): Vector2 {
        val index = indexOf(element)
        return Vector2(data!![index], data!![index + 1])
    }

    override fun set(element: E, value: Vector2) {
        val index = indexOf(element)
        data!![index] = value.x
        data!![index + 1] = value.y
    }

    override fun equals(a: E, b: E): Boolean {
        val aIndex = indexOf(a)
        val bIndex = indexOf(b)
        return data!![aIndex] == data!![bIndex] && data!![aIndex + 1] == data!![bIndex + 1]
    }

    override fun alloc(size: Int): FloatArray {
        return FloatArray(size)
    }

}