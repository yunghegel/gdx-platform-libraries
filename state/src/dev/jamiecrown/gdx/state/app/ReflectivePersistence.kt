package dev.jamiecrown.gdx.state.app

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.Vector4
import dev.jamiecrown.gdx.state.Persist
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType

/**
 * Utilities to persist @Persist-annotated fields on any object using StateWriter/StateReader.
 */
object ReflectivePersistence {

    fun writeFields(instance: Any, writer: StateWriter) {
        for (f in collectPersistFields(instance)) {
            val key = persistKey(f)
            val v = f.get(instance) ?: continue
            when (v) {
                is String -> writer.put(key, v)
                is Int -> writer.putInt(key, v)
                is Float -> writer.putFloat(key, v)
                is Boolean -> writer.putBoolean(key, v)
                is Long, is Double, is Short, is Byte -> writer.put(key, v.toString())
                is Enum<*> -> writer.put(key, v.name)
                is Vector2 -> writer.put(key, v)
                is Vector3 -> writer.put(key, v)
                is Vector4 -> writer.put(key, v)
                is Color -> writer.put(key, v)
                is Quaternion -> writer.put(key, v)
                is Matrix3 -> writer.put(key, v)
                is Matrix4 -> writer.put(key, v)
                is Map<*, *> -> writeMap(key, v, writer, f)
                else -> writer.put(key, v.toString()) // fallback
            }
        }
    }

    fun readFields(instance: Any, reader: StateReader) {
        for (f in collectPersistFields(instance)) {
            val key = persistKey(f)
            val type = f.type
            val value: Any? = when {
                type == String::class.java -> reader.get(key)
                type == Int::class.javaPrimitiveType || type == Int::class.java -> reader.getInt(key)
                type == Float::class.javaPrimitiveType || type == Float::class.java -> reader.getFloat(key)
                type == Boolean::class.javaPrimitiveType || type == Boolean::class.java -> reader.getBoolean(key)
                type == Long::class.javaPrimitiveType || type == Long::class.java -> reader.get(key)?.toLongOrNull()
                type == Double::class.javaPrimitiveType || type == Double::class.java -> reader.get(key)?.toDoubleOrNull()
                type == Short::class.javaPrimitiveType || type == Short::class.java -> reader.get(key)?.toShortOrNull()
                type == Byte::class.javaPrimitiveType || type == Byte::class.java -> reader.get(key)?.toByteOrNull()
                type.isEnum -> reader.get(key)?.let { n -> java.lang.Enum.valueOf(type as Class<out Enum<*>>, n) }
                type == Vector2::class.java -> reader.getVector2(key)
                type == Vector3::class.java -> reader.getVector3(key)
                type == Vector4::class.java -> reader.getVector4(key)
                type == Color::class.java -> reader.getColor(key)
                type == Quaternion::class.java -> reader.getQuaternion(key)
                type == Matrix3::class.java -> reader.getMatrix3(key)
                type == Matrix4::class.java -> reader.getMatrix4(key)
                Map::class.java.isAssignableFrom(type) -> readMap(key, f, reader)
                else -> reader.get(key)
            }
            if (value != null) f.set(instance, value)
        }
    }

    private fun collectPersistFields(instance: Any): List<Field> {
        val result = mutableListOf<Field>()
        var c: Class<*>? = instance.javaClass
        while (c != null && c != Any::class.java) {
            for (f in c.declaredFields) {
                if (f.getAnnotation(Persist::class.java) != null) {
                    f.isAccessible = true
                    result += f
                }
            }
            c = c.superclass
        }
        return result.sortedBy { it.name }
    }

    private fun persistKey(f: Field): String {
        val ann = f.getAnnotation(Persist::class.java)
        return ann?.key?.takeIf { it.isNotBlank() } ?: f.name
    }

    private fun writeMap(key: String, mapValue: Map<*, *>, writer: StateWriter, field: Field) {
        // Attempt to decide by generic value type if available; otherwise infer by first value instance
        val (keyType, valueType) = mapGenericTypes(field)
        @Suppress("UNCHECKED_CAST")
        when {
            valueType == Int::class.java || mapValue.values.firstOrNull() is Int ->
                writer.putMapInt(key, (mapValue as Map<String, Int>))
            valueType == Float::class.java || mapValue.values.firstOrNull() is Float ->
                writer.putMapFloat(key, (mapValue as Map<String, Float>))
            valueType == Boolean::class.java || mapValue.values.firstOrNull() is Boolean ->
                writer.putMapBoolean(key, (mapValue as Map<String, Boolean>))
            valueType == String::class.java || mapValue.values.firstOrNull() is String ->
                writer.putMapString(key, (mapValue as Map<String, String>))
            else -> {
                // Fallback: stringify values
                val strMap = linkedMapOf<String, String>()
                for ((k, v) in mapValue) if (k is String && v != null) strMap[k] = v.toString()
                writer.putMapString(key, strMap)
            }
        }
    }

    private fun readMap(key: String, field: Field, reader: StateReader): Any? {
        val (_, valueType) = mapGenericTypes(field)
        return when (valueType) {
            Int::class.java -> reader.getMapInt(key)
            Float::class.java -> reader.getMapFloat(key)
            Boolean::class.java -> reader.getMapBoolean(key)
            String::class.java -> reader.getMapString(key)
            else -> {
                // Fallback to inspecting the persisted type tag
                when (reader.getTypeTag(key)) {
                    "mapInt" -> reader.getMapInt(key)
                    "mapFloat" -> reader.getMapFloat(key)
                    "mapBool" -> reader.getMapBoolean(key)
                    "mapStr" -> reader.getMapString(key)
                    else -> reader.getMapString(key)
                }
            }
        }
    }

    private fun mapGenericTypes(field: Field): Pair<Class<*>?, Class<*>?> {
        val gt = field.genericType
        if (gt is ParameterizedType) {
            val args = gt.actualTypeArguments
            val k = (args.getOrNull(0) as? Class<*>)
            val v = (args.getOrNull(1) as? Class<*>)
            return k to v
        }
        return null to null
    }
}
