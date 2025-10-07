package dev.jamiecrown.gdx.geometry.data.attribute

import dev.jamiecrown.gdx.geometry.core.Element
import dev.jamiecrown.gdx.geometry.data.ElementData
import kotlin.collections.get

@Suppress("UNCHECKED_CAST")
abstract class ElementAttribute<E, T, TArr>(override val numComponents: Int, override val name: String) :
    Property<E, T, TArr> where E : Element, TArr : Any {

    override var data: TArr? = null


    //    [n elements ... size -n elements]
    fun arrayToString(): String {
        val sb = StringBuilder()
        sb.append("[")
        if (data is FloatArray) {
            val size = (data as FloatArray).size
            val n = 3
            for (i in 0 until n) {
                sb.append((data as FloatArray)[i])
                if (i < n - 1) {
                    sb.append(", ")
                }
            }
            sb.append(" ... ")
            for (i in size - n until size) {
                sb.append((data as FloatArray)[i])
                if (i < size - 1) {
                    sb.append(", ")
                }
            }
        } else if (data is DoubleArray) {
            val size = (data as DoubleArray).size
            val n = 3
            for (i in 0 until n) {
                sb.append((data as DoubleArray)[i])
                if (i < n - 1) {
                    sb.append(", ")
                }
            }
            sb.append(" ... ")
            for (i in size - n until size) {
                sb.append((data as DoubleArray)[i])
                if (i < size - 1) {
                    sb.append(", ")
                }
            }
        } else if (data is IntArray) {
            val size = (data as IntArray).size
            val n = 3
            for (i in 0 until n) {
                sb.append((data as IntArray)[i])
                if (i < n - 1) {
                    sb.append(", ")
                }
            }
            sb.append(" ... ")
            for (i in size - n until size) {
                sb.append((data as IntArray)[i])
                if (i < size - 1) {
                    sb.append(", ")
                }
            }
        } else if (data is LongArray) {
            val size = (data as LongArray).size
            val n = 3
            for (i in 0 until n) {
                sb.append((data as LongArray)[i])
                if (i < n - 1) {
                    sb.append(", ")
                }
            }
            sb.append(" ... ")
            for (i in size - n until size) {
                sb.append((data as LongArray)[i])
                if (i < size - 1) {
                    sb.append(", ")
                }
            }
        } else if (data is ShortArray) {
            val size = (data as ShortArray).size
            val n = 3
            for (i in 0 until n) {
                sb.append((data as ShortArray)[i])
                if (i < n - 1) {
                    sb.append(", ")
                }
            }
            sb.append(" ... ")
            for (i in size - n until size) {
                sb.append((data as ShortArray)[i])
                if (i < size - 1) {
                    sb.append(", ")
                }
            }
        } else if (data is Array<*>) {
            val size = (data as Array<*>).size
            val n = 3
            for (i in 0 until n) {
                sb.append((data as Array<*>)[i])
                if (i < n - 1) {
                    sb.append(", ")
                }
            }
            sb.append(" ... ")
            for (i in size - n until size) {
                sb.append((data as Array<*>)[i])
                if (i < size - 1) {
                    sb.append(", ")
                }
            }

        }
        sb.append("]")
        return sb.toString()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Attribute: $name\n")
        sb.append(" | Num components: $numComponents\n")
        sb.append(" | Data: \n${arrayToString()}\n")
        return sb.toString()
    }

    companion object {
        fun <E : Element, T> getAttribute(
            name: String?,
            meshData: ElementData<E>,
            arrayType: Class<Array<T>>
        ): ElementAttribute<E, T, Array<T>>? {
            return meshData.attributes[name] as ElementAttribute<E, T, Array<T>>?
        }
    }


}