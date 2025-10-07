package dev.jamiecrown.gdx.geometry.data


import dev.jamiecrown.gdx.geometry.core.Element
import dev.jamiecrown.gdx.geometry.data.attribute.AbstractProperty

class ElementData<E : Element>(override val producer: (Array<out Any?>) -> E) : Iterable<E>,
    AbstractObjectData<E, AbstractProperty<E, *, *>>(producer) {

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("|\tElements: $size\n|\tAttributes: ${attributes.size}\n")
        attributes.forEach { (_, attribute) ->
            sb.append("${attribute}\n")
        }
        return sb.toString()
    }

}