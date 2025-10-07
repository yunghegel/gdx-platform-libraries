package dev.jamiecrown.gdx.geometry.data

import dev.jamiecrown.gdx.geometry.core.Indexed
import dev.jamiecrown.gdx.geometry.data.attribute.Property
import kotlin.math.ceil

interface ObjectData<T : Indexed, P : Property<T, *, *>> : Iterable<T> {

    val elements: ArrayList<T>

    val attributes: Map<String, P>

    var numVirtual: Int

    var modCount: Int

    var arraySize: Int

    val size: Int
        get() = elements.size - numVirtual

    val total: Int
        get() = elements.size

    val producer: (Array<out Any?>) -> T

    fun create(vararg args: Any): T {
        val newIndex = elements.size
        if (newIndex >= arraySize) {
            val capacity = ceil(arraySize * GROWTH_FACTOR).toInt()
            ensureCapacity(capacity)
        }
        val element = producer(args)
        element.index = newIndex
        elements.add(element)
        modCount++
        return element
    }

    fun createVirtual(vararg args: Any): T {
        val newIndex = elements.size
        if (newIndex >= arraySize) {
            val capacity = ceil(arraySize * GROWTH_FACTOR).toInt()
            ensureCapacity(capacity)
        }
        val element = producer(args)
        element.index = newIndex
        element.flags.set(Indexed.FLAG_VIRTUAL, true)
        elements.add(element)
        numVirtual++
        modCount++
        return element
    }

    fun destroy(value: T) {
        val index = value.index
        if (index == -1) {
            return
        }
        if (value.flags.get(Indexed.FLAG_VIRTUAL)) {
            numVirtual--
        }

        val lastIndex = elements.size - 1
        if (index != lastIndex) {
            val lastElement = elements[lastIndex]
            copyAttributes(lastElement, value)
            elements.set(index, lastElement)
            lastElement.index = index
        }
        elements.removeAt(lastIndex)
        value.reset()
        modCount++
    }

    operator fun get(name: String): P {
        return attributes[name] ?: throw IllegalArgumentException("Attribute with name $name does not exist")
    }

    operator fun get(index: Int): T {
        return elements[index]
    }

    operator fun get(index: Short): T {
        return elements[index.toInt()]
    }

    fun copyAttributes(from: T, to: T) {
        attributes.forEach { _, attribute ->
            attribute.copy(from, to)
        }
    }

    fun reserveCapacity(count: Int) {
        ensureCapacity(elements.size + count)
    }


    fun resize(size: Int, copyLength: Int) {
        require(size > 0) { "New size must be positive" }
        attributes.forEach { _, attribute ->
            attribute.realloc(size, copyLength)
        }
        arraySize = size
    }

    fun ensureCapacity(minCapcity: Int) {
        elements.ensureCapacity(minCapcity)
        if (arraySize < minCapcity) {
            resize(minCapcity, arraySize)
        }
    }

    companion object {
        const val INITIAL_ARRAY_SIZE = 32
        const val GROWTH_FACTOR = 1.5f
    }

}