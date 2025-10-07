package dev.jamiecrown.gdx.geometry.data.attribute

import com.badlogic.gdx.math.Vector4
import dev.jamiecrown.gdx.geometry.core.Indexed

class Vector4Attribute<E : Indexed>(name: String) : AbstractProperty<E, Vector4, FloatArray>(4, name) {

    override fun get(element: E): Vector4 {
        val index = indexOf(element)
        return Vector4(data!![index], data!![index + 1], data!![index + 2], data!![index + 3])
    }

    override fun set(element: E, value: Vector4) {
        val index = indexOf(element)
        data!![index] = value.x
        data!![index + 1] = value.y
        data!![index + 2] = value.z
        data!![index + 3] = value.w
    }

    override fun equals(a: E, b: E): Boolean {
        val aIndex = indexOf(a)
        val bIndex = indexOf(b)
        return data!![aIndex] == data!![bIndex] && data!![aIndex + 1] == data!![bIndex + 1] && data!![aIndex + 2] == data!![bIndex + 2] && data!![aIndex + 3] == data!![bIndex + 3]
    }

    override fun alloc(size: Int): FloatArray {
        return FloatArray(size)
    }

}