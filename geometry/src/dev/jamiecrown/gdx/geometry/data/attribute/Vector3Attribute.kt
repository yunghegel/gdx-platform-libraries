package dev.jamiecrown.gdx.geometry.data.attribute

import com.badlogic.gdx.math.Vector3
import dev.jamiecrown.gdx.geometry.core.Indexed

class Vector3Attribute<E : Indexed>(name: String) : AbstractProperty<E, Vector3, FloatArray>(3, name) {

    override fun get(element: E): Vector3 {
        val index = indexOf(element)
        return Vector3(data!![index], data!![index + 1], data!![index + 2])
    }

    override fun set(element: E, value: Vector3) {
        val index = indexOf(element)
        data!![index] = value.x
        data!![index + 1] = value.y
        data!![index + 2] = value.z
    }

    override fun equals(a: E, b: E): Boolean {
        val aIndex = indexOf(a)
        val bIndex = indexOf(b)
        return data!![aIndex] == data!![bIndex] && data!![aIndex + 1] == data!![bIndex + 1] && data!![aIndex + 2] == data!![bIndex + 2]
    }

    override fun alloc(size: Int): FloatArray {
        return FloatArray(size)
    }

}