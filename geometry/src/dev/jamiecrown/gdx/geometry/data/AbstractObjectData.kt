package dev.jamiecrown.gdx.geometry.data

import dev.jamiecrown.gdx.geometry.core.Indexed
import dev.jamiecrown.gdx.geometry.data.ObjectData.Companion.INITIAL_ARRAY_SIZE
import dev.jamiecrown.gdx.geometry.data.attribute.AbstractProperty
import dev.jamiecrown.gdx.geometry.data.attribute.Property

abstract class AbstractObjectData<T : Indexed, P : Property<T, *, *>>(override val producer: (Array<out Any?>) -> T) :
    ObjectData<T, P> {

    override val elements: ArrayList<T> = ArrayList<T>()

    override val attributes = mutableMapOf<String, P>()

    override var numVirtual = 0

    override var modCount = 0

    override var arraySize = INITIAL_ARRAY_SIZE

    fun addAttribute(attribute: P) {
        require(!attributes.containsKey(attribute.name)) { "Attribute with name ${attribute.name} already exists" }
        require(attribute.data == null) { "Attribute data must be null" }
        val oldArr = attribute.allocReplace(arraySize)
        assert(oldArr == null && attribute.data != null)
        attributes[attribute.name] = attribute
    }

    fun removeAttribute(name: String) {
        attributes.remove(name)
    }

    override fun iterator(): Iterator<T> {
        return ObjectIterator()
    }

    companion object {
        fun <T : Indexed, A : Any, P : AbstractProperty<T, A, Array<A>>, D : AbstractObjectData<T, P>> getAttribute(
            name: String,
            data: D
        ): P {
            return data.attributes[name] as P
        }
    }

    protected inner class ObjectIterator() : MutableIterator<T> {
        private var index = 0
        private var lastRet = -1
        private var expectedModCount = modCount

        override fun hasNext(): Boolean {
            return index < elements.size
        }

        override fun next(): T {
            checkForComodification()
            if (index >= elements.size) {
                throw NoSuchElementException()
            }
            lastRet = index
            return elements[index++]
        }

        override fun remove() {
            if (lastRet < 0) {
                throw IllegalStateException()
            }
            checkForComodification()
            try {
                elements.removeAt(lastRet)
                index = lastRet
                lastRet = -1
                expectedModCount = modCount
            } catch (e: IndexOutOfBoundsException) {
                throw ConcurrentModificationException()
            }
        }

        private fun checkForComodification() {
            if (modCount != expectedModCount) {
                throw ConcurrentModificationException()
            }
        }
    }


}