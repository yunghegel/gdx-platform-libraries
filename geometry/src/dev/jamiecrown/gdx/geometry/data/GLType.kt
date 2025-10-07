package dev.jamiecrown.gdx.geometry.data

import com.badlogic.gdx.graphics.GL30
import org.lwjgl.BufferUtils
import java.nio.Buffer

enum class GLType(val glUint:Int) {
    VECTOR3(GL30.GL_FLOAT_VEC3),
    VECTOR2(GL30.GL_FLOAT_VEC2),
    FLOAT(GL30.GL_FLOAT),
    INT(GL30.GL_INT),
    SHORT(GL30.GL_SHORT),
    BOOLEAN(GL30.GL_BOOL),
    MATRIX4(GL30.GL_FLOAT_MAT4),
    MATRIX3(GL30.GL_FLOAT_MAT3),
    COLOR(GL30.GL_FLOAT_VEC4);

    val size: Int
        get() = when(this) {
            VECTOR3 -> 3
            VECTOR2 -> 2
            FLOAT -> 1
            INT -> 1
            SHORT -> 1
            BOOLEAN -> 1
            MATRIX4 -> 16
            MATRIX3 -> 9
            COLOR -> 4
        }

    val bytes : Int
        get() = when(this) {
            VECTOR3 -> 3 * 4
            VECTOR2 -> 2 * 4
            FLOAT -> 4
            INT -> 4
            SHORT -> 2
            BOOLEAN -> 1
            MATRIX4 -> 16 * 4
            MATRIX3 -> 9 * 4
            COLOR -> 4 * 4
        }

    fun alloc(capacity: Int): Buffer {
        return BufferUtils.createByteBuffer(capacity * bytes)
    }

}