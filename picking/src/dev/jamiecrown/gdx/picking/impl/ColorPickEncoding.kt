package dev.jamiecrown.gdx.picking.impl

/**
 * 24-bit color encoding helper. Allows packing up to 4 primitive types (2 bits)
 * and a 22-bit index into RGB bytes.
 */
object ColorPickEncoding {
    const val TYPE_SHIFT = 22
    const val INDEX_MASK = (1 shl TYPE_SHIFT) - 1

    fun encode(typeCode: Int, localIndexZeroBased: Int): Int {
        require(typeCode in 1..3) { "typeCode must be 1..3" }
        val id = ((typeCode and 0x3) shl TYPE_SHIFT) or ((localIndexZeroBased + 1) and INDEX_MASK)
        return id and 0xFFFFFF
    }

    fun encodeToColorFloats(typeCode: Int, localIndexZeroBased: Int): FloatArray {
        val id = encode(typeCode, localIndexZeroBased)
        val r = (id shr 16) and 0xFF
        val g = (id shr 8) and 0xFF
        val b = id and 0xFF
        return floatArrayOf(r / 255f, g / 255f, b / 255f, 1f)
    }

    fun decodeFromBytes(r: Int, g: Int, b: Int): Pair<Int, Int>? {
        val id = (r shl 16) or (g shl 8) or b
        if (id == 0) return null
        val typeCode = (id shr TYPE_SHIFT) and 0x3
        val localIndexPlusOne = id and INDEX_MASK
        if (localIndexPlusOne == 0) return null
        val localIndex = localIndexPlusOne - 1
        return typeCode to localIndex
    }
}

enum class PrimitiveType { Vertex, Edge, Face }

data class PrimitivePick(val primitiveType: PrimitiveType, val primitiveIndex: Int)
