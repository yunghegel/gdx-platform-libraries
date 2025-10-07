package dev.jamiecrown.gdx.geometry.data.attribute

import dev.jamiecrown.gdx.core.util.floatEquals
import dev.jamiecrown.gdx.geometry.core.Indexed


class FloatAttribute<E : Indexed>(name: String) : AbstractProperty<E, Float, Array<Float>>(1, name) {

        override fun get(element: E): Float {
            return data!![indexOf(element)]
        }

        override fun set(element: E, value: Float) {
            data!![indexOf(element)] = value
        }

        override fun equals(a: E, b: E): Boolean {
            val aIndex = indexOf(a)
            val bIndex = indexOf(b)
            return floatEquals(data!![aIndex], data!![bIndex])
        }

        override fun alloc(size: Int): Array<Float> {
            return Array(size) { 0f }
        }

}