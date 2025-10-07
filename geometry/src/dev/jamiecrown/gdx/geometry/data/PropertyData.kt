package dev.jamiecrown.gdx.geometry.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import dev.jamiecrown.gdx.geometry.core.Indexed
import dev.jamiecrown.gdx.geometry.data.attribute.FloatAttribute
import dev.jamiecrown.gdx.geometry.data.attribute.FloatTupleAttribute
import dev.jamiecrown.gdx.geometry.data.attribute.ObjectAttribute
import dev.jamiecrown.gdx.geometry.data.attribute.Property
import dev.jamiecrown.gdx.geometry.data.attribute.ShortAttribute
import dev.jamiecrown.gdx.geometry.data.attribute.ShortTupleAttribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector2Attribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector3Attribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector4Attribute
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor
import java.io.*
import kotlin.collections.get
import kotlin.reflect.KFunction

@Serializable
data class PropertyData(
    val name: String,
    val type: String,
    val numComponents: Int,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PropertyData
        return name == other.name && type == other.type &&
                numComponents == other.numComponents && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + numComponents
        result = 31 * result + data.contentHashCode()
        return result
    }
}

@Serializable
data class SerializedObjectData(
    val size: Int,
    val properties: List<PropertyData>
)

class PropertySerializationException(message: String, cause: Throwable? = null) : Exception(message, cause)

class PropertySerializer {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
        }

        private val PROPERTY_TYPES = mapOf(

            "FloatAttribute" to ::serializeFloatAttribute,
            "ObjectAttribute" to ::serializeObjectAttribute,
            "FloatTupleAttribute" to ::serializeFloatTupleAttribute,
            "ShortTupleAttribute" to ::serializeShortTupleAttribute,
            "Vector2Attribute" to ::serializeVector2Attribute,
            "Vector3Attribute" to ::serializeVector3Attribute,
            "Vector4Attribute" to ::serializeVector4Attribute,
            "ShortAttribute" to ::serializeShortAttribute

        )

        fun <T : Indexed> serialize(data: ObjectData<T, *>): String {
            val properties = mutableListOf<PropertyData>()

            data.attributes.forEach { (_, property) ->
                validateSerializable(property)
                val propertyData = serializeProperty(property)
                properties.add(propertyData)
            }

            val serializedData = SerializedObjectData(
                size = data.size,
                properties = properties
            )

            return json.encodeToString(SerializedObjectData.serializer(), serializedData)
        }

        fun <T : Indexed> deserialize(
            jsonString: String,
            producer: (Array<out Any?>) -> T
        ): ObjectData<T, Property<T, *, *>> {
            val serializedData = json.decodeFromString<SerializedObjectData>(jsonString)

            val objectData = createObjectData(producer)

            serializedData.properties.forEach { propertyData ->
                val property = deserializeProperty<T>(propertyData)
                validateSerializable(property)
                objectData.attributes
            }

            objectData.ensureCapacity(serializedData.size)
            return objectData
        }

        private fun validateSerializable(property: Property<*, *, *>) {
            val kClass = property::class
            if (!kClass.hasAnnotation<Serializable>()) {
                if (property is ObjectAttribute<*, *>) {
                    return
                }
            }

            // For ObjectAttribute, validate that the contained type is also serializable
            if (property is ObjectAttribute<*, *>) {
                val containedType = property.data?.firstOrNull()?.let { it::class }
                if (containedType != null && !containedType.hasAnnotation<Serializable>()) {
                    throw PropertySerializationException(
                        "ObjectAttribute contains non-serializable type ${containedType.simpleName}"
                    )
                }
            }
        }

        private fun serializeFloatAttribute(property: Property<*, *, *>): PropertyData {
            val floatArr = property.data as Array<Float>
            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)

            floatArr.forEach { dos.writeFloat(it) }

            return PropertyData(
                property.name,
                "FloatAttribute",
                property.numComponents,
                baos.toByteArray()
            )
        }

        @OptIn(InternalSerializationApi::class)
        private fun serializeObjectAttribute(property: Property<*, *, *>): PropertyData {
            val objectArr = (property.data as? Array<*>)
                ?: throw PropertySerializationException("Property data is not an Array")

            if (objectArr.isEmpty()) {
                throw PropertySerializationException("ObjectAttribute contains an empty array")
            }

            val firstObj = objectArr.firstOrNull()
                ?: throw PropertySerializationException("Unable to determine type of objects in ObjectAttribute")

            val type = firstObj::class.qualifiedName
                ?: throw PropertySerializationException("Unable to resolve qualified name of object class")

            val objClass = try {
                Class.forName(type).kotlin
            } catch (ex: ClassNotFoundException) {
                throw PropertySerializationException("Class not found for type: $type", ex)
            }

            if (!objClass.hasAnnotation<Serializable>()) {
                throw PropertySerializationException("ObjectAttribute contains non-serializable type: $type")
            }

            val serializer = try {
                objClass.serializer()
            } catch (ex: Exception) {
                throw PropertySerializationException("Failed to retrieve serializer for type: $type", ex)
            }

            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)

            objectArr.forEach { obj ->
                if (obj != null) {
                    val json = Json.encodeToString(serializer as KSerializer<Any>, obj)
                    dos.writeBytes(json) // Write serialized JSON content to the output stream
                }
            }

            return PropertyData(
                property.name,
                "ObjectAttribute",
                property.numComponents,
                baos.toByteArray()
            )
        }

        private fun <T : Indexed> deserializeProperty(propertyData: PropertyData): Property<T, *, *> {
            val className = "org.yunghegel.gdx.geom.data.attribute.${propertyData.type}"
            val kClass = Class.forName(className).kotlin

            if (!kClass.hasAnnotation<Serializable>()) {
                throw PropertySerializationException("Property type ${propertyData.type} is not marked as @Serializable")
            }

            val constructor = kClass.primaryConstructor
                ?: throw PropertySerializationException("No primary constructor found for ${propertyData.type}")

            return when (propertyData.type) {
                "FloatAttribute" -> createFloatAttribute<T>(propertyData, constructor)
                "ObjectAttribute" -> createObjectAttribute<T>(propertyData, constructor)
                "ShortTupleAttribute" -> createShortTupleAttribute<T>(propertyData, constructor)
                "Vector2Attribute" -> createVector2Attribute<T>(propertyData, constructor)
                "Vector3Attribute" -> createVector3Attribute<T>(propertyData, constructor)
                "ShortAttribute" -> createShortAttribute<T>(propertyData, constructor)
                "Vector4Attribute" -> createVector4Attribute<T>(propertyData, constructor)
                "FloatTupleAttribute" -> createFloatTupleAttribute<T>(propertyData, constructor)

                else -> throw PropertySerializationException("Unknown property type: ${propertyData.type}")
            }
        }

        private fun <T : Indexed> createObjectAttribute(
            propertyData: PropertyData,
            constructor: KFunction<*>
        ): ObjectAttribute<T, *> {
            val attribute = constructor.call(propertyData.name) as ObjectAttribute<T, Any?>
            val dis = DataInputStream(ByteArrayInputStream(propertyData.data))

            try {
                val objects = mutableListOf<Any?>()
                while (dis.available() > 0) {
                    val objJson = dis.readUTF()
                    val obj = if (objJson.isNotEmpty()) {
                        // Need to determine the actual type here - might need to store type information
                        // in the PropertyData or use type tokens
                        json.decodeFromString<Any>(objJson)
                    } else null
                    objects.add(obj)
                }
                attribute.data = objects.toTypedArray()
            } catch (e: SerializationException) {
                throw PropertySerializationException("Failed to deserialize object", e)
            }

            return attribute
        }

        private fun <T : Indexed> createFloatAttribute(
            propertyData: PropertyData,
            constructor: KFunction<*>
        ): FloatAttribute<T> {
            val attribute = constructor.call(propertyData.name) as FloatAttribute<T>
            val dis = DataInputStream(ByteArrayInputStream(propertyData.data))
            try {
                attribute.data = attribute.allocReplace(propertyData.numComponents)
                for (i in 0 until propertyData.numComponents) {
                    attribute.data!![i] = dis.readFloat()
                }
            } catch (e: IOException) {
                throw PropertySerializationException("Failed to deserialize float array", e)
            }
            return attribute
        }

        private fun <T : Indexed> createFloatTupleAttribute(
            propertyData: PropertyData,
            constructor: KFunction<*>
        ): FloatTupleAttribute<T> {
            val attribute = constructor.call(propertyData.name) as FloatTupleAttribute<T>
            val dis = DataInputStream(ByteArrayInputStream(propertyData.data))
            try {
                attribute.data = attribute.allocReplace(propertyData.numComponents)
                for (i in 0 until propertyData.numComponents) {
                    val arr = FloatArray(attribute.numComponents)
                    for (j in 0 until attribute.numComponents) {
                        arr[j] = dis.readFloat()
                    }
                }
            } catch (e: IOException) {
                throw PropertySerializationException("Failed to deserialize float tuple array", e)
            }
            return attribute
        }

        private fun <T : Indexed> createShortAttribute(
            propertyData: PropertyData,
            constructor: KFunction<*>
        ): ShortAttribute<T> {
            val attribute = constructor.call(propertyData.name) as ShortAttribute<T>
            val dis = DataInputStream(ByteArrayInputStream(propertyData.data))
            try {
                attribute.data = attribute.allocReplace(propertyData.numComponents)
                for (i in 0 until propertyData.numComponents) {
                    attribute.data!![i] = dis.readShort()
                }
            } catch (e: IOException) {
                throw PropertySerializationException("Failed to deserialize short array", e)
            }
            return attribute
        }

        private fun <T : Indexed> createShortTupleAttribute(
            propertyData: PropertyData,
            constructor: KFunction<*>
        ): ShortTupleAttribute<T> {
            val attribute = constructor.call(propertyData.name) as ShortTupleAttribute<T>
            val dis = DataInputStream(ByteArrayInputStream(propertyData.data))
            try {
                attribute.data = attribute.allocReplace(propertyData.numComponents)
                for (i in 0 until propertyData.numComponents) {
                    val arr = ShortArray(attribute.numComponents)
                    for (j in 0 until attribute.numComponents) {
                        arr[j] = dis.readShort()
                    }
                }
            } catch (e: IOException) {
                throw PropertySerializationException("Failed to deserialize short tuple array", e)
            }
            return attribute
        }

        private fun <T : Indexed> createVector2Attribute(
            propertyData: PropertyData,
            constructor: KFunction<*>
        ): Vector2Attribute<T> {
            val attribute = constructor.call(propertyData.name) as Vector2Attribute<T>
            val dis = DataInputStream(ByteArrayInputStream(propertyData.data))
            try {
                attribute.data = attribute.allocReplace(propertyData.numComponents)
                for (i in 0 until propertyData.numComponents) {
                    attribute.data!![i] = dis.readFloat()
                }
            } catch (e: IOException) {
                throw PropertySerializationException("Failed to deserialize float array", e)
            }
            return attribute

        }

        private fun <T : Indexed> createVector3Attribute(
            propertyData: PropertyData,
            constructor: KFunction<*>
        ): Vector3Attribute<T> {
            val attribute = constructor.call(propertyData.name) as Vector3Attribute<T>
            val dis = DataInputStream(ByteArrayInputStream(propertyData.data))
            try {
                attribute.data = attribute.allocReplace(propertyData.numComponents)
                for (i in 0 until propertyData.numComponents) {
                    attribute.data!![i] = dis.readFloat()
                }
            } catch (e: IOException) {
                throw PropertySerializationException("Failed to deserialize float array", e)
            }
            return attribute
        }

        private fun <T : Indexed> createVector4Attribute(
            propertyData: PropertyData,
            constructor: KFunction<*>
        ): Vector4Attribute<T> {
            val attribute = constructor.call(propertyData.name) as Vector4Attribute<T>
            val dis = DataInputStream(ByteArrayInputStream(propertyData.data))
            try {
                attribute.data = attribute.allocReplace(propertyData.numComponents)
                for (i in 0 until propertyData.numComponents) {
                    attribute.data!![i] = dis.readFloat()
                }
            } catch (e: IOException) {
                throw PropertySerializationException("Failed to deserialize float array", e)
            }
            return attribute
        }


        private fun serializeFloatTupleAttribute(property: Property<*, *, *>): PropertyData {
            val floatArr = property.data as FloatArray
            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)


            floatArr.forEach { dos.writeFloat(it) }

            return PropertyData(
                property.name,
                "FloatTupleAttribute",
                property.numComponents,
                baos.toByteArray()
            )
        }

        private fun serializeShortTupleAttribute(property: Property<*, *, *>): PropertyData {
            val shortArr = property.data as Array<ShortArray>
            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)
            shortArr.forEach { arr ->
                arr.forEach { dos.writeShort(it.toInt()) }
            }

            return PropertyData(
                property.name,
                "ShortTupleAttribute",
                property.numComponents,
                baos.toByteArray()
            )

        }


        private fun serializeVector2Attribute(property: Property<*, *, *>): PropertyData {
            val floatArr = property.data as FloatArray
            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)
            floatArr.forEach { dos.writeFloat(it) }

            return PropertyData(
                property.name,
                "Vector2Attribute",
                property.numComponents,
                baos.toByteArray()
            )
        }

        private fun serializeVector3Attribute(property: Property<*, *, *>): PropertyData {
            val floatArr = property.data as FloatArray
            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)

            floatArr.forEach { dos.writeFloat(it) }


            return PropertyData(
                property.name,
                "Vector3Attribute",
                property.numComponents,
                baos.toByteArray()
            )
        }

        private fun serializeVector4Attribute(property: Property<*, *, *>): PropertyData {
            val floatArr = property.data as Array<FloatArray>
            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)
            floatArr.forEach { arr ->
                arr.forEach { dos.writeFloat(it) }
            }
            return PropertyData(
                property.name,
                "Vector4Attribute",
                property.numComponents,
                baos.toByteArray()
            )
        }

        private fun serializeShortAttribute(property: Property<*, *, *>): PropertyData {
            val shortArr = property.data as Array<ShortArray>
            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)
            shortArr.forEach { arr ->
                arr.forEach { dos.writeShort(it.toInt()) }
            }
            return PropertyData(
                property.name,
                "ShortAttribute",
                property.numComponents,
                baos.toByteArray()
            )
        }

        private fun <T : Indexed> serializeProperty(property: Property<T, *, *>): PropertyData {
            val propertyType = PROPERTY_TYPES[property::class.simpleName]
            if (propertyType != null) {
                return propertyType(property)
            } else {
                throw PropertySerializationException("Unknown property type: ${property::class.simpleName}")
            }
        }

        private fun <T : Indexed> createObjectData(
            producer: (Array<out Any?>) -> T
        ): ObjectData<T, Property<T, *, *>> {
            return object : AbstractObjectData<T, Property<T, *, *>>(producer) {}
        }

        // Additional serialization methods for other property types...
    }
}

// Extension functions
fun <T : Indexed> ObjectData<T, *>.serializeToJson(): String {
    return PropertySerializer.serialize(this)
}

fun <T : Indexed> String.deserializeFromJson(producer: (Array<out Any?>) -> T): ObjectData<T, Property<T, *, *>> {
    return PropertySerializer.deserialize(this, producer)
}