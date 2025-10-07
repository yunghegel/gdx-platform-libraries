package dev.jamiecrown.gdx.geometry.data.attribute

import dev.jamiecrown.gdx.geometry.core.Indexed

import dev.jamiecrown.gdx.geometry.data.ObjectData


class ObjectAttribute<E : Indexed, T>(name: String, private val allocator: ArrayAllocator<T>) :
    AbstractProperty<E, T, Array<T?>>(1, name) {

    fun interface ArrayAllocator<T> {
        fun alloc(size: Int): Array<T?>
    }


    override fun set(element: E, value: T) {
        data!![indexOf(element)] = value
    }

    override fun get(element: E): T? {
        return data!![indexOf(element)]
    }


    override fun equals(a: E, b: E): Boolean {
        return a === b
    }


    override fun alloc(size: Int): Array<T?> {
        return allocator.alloc(size)
    }


    companion object {


        fun <E : Indexed, T : Any> get(
            name: String,
            data: ObjectData<E, *>,
        ): ObjectAttribute<E, T>? {
            return data.attributes[name] as? ObjectAttribute<E, T>
        }

        fun <E : Indexed, T : Any> getOrCreate(
            name: String,
            data: ObjectData<E, *>,
            allocator: ArrayAllocator<T>
        ): ObjectAttribute<E, T> {
            val attribute: ObjectAttribute<E, T>? = get(name, data)
            if (attribute == null) {
                return ObjectAttribute<E, T>(name, allocator)
            }
            return attribute
        }
    }
}