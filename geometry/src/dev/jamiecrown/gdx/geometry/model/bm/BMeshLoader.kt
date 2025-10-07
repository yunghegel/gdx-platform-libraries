package dev.jamiecrown.gdx.geometry.model.bm

import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.Vector4
import dev.jamiecrown.gdx.geometry.data.attribute.AbstractProperty
import dev.jamiecrown.gdx.geometry.data.attribute.FloatAttribute
import dev.jamiecrown.gdx.geometry.data.attribute.FloatTupleAttribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector2Attribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector3Attribute
import dev.jamiecrown.gdx.geometry.data.attribute.Vector4Attribute
import dev.jamiecrown.gdx.geometry.model.bm.struct.BMVertex
import java.io.File

class BMeshLoader(val mesh : Mesh) {

    var resultString = ""

    fun load() : BMesh {

        val bmesh = BMesh()
        bmesh.initializeMesh(mesh)

        val indicesAmount = mesh.numIndices
        val indicesArray = ShortArray(indicesAmount)
        mesh.getIndices(indicesArray)

        val stride = mesh.vertexSize / 4
        val verticesAmount = mesh.numVertices
        val vertexCount = verticesAmount * stride
        val verticesArray = FloatArray(vertexCount)
        mesh.getVertices(verticesArray)

        val attributes = mesh.vertexAttributes

        println(indicesArray.contentToString())

        for (i in 0 until verticesAmount) {
            val vertex = bmesh.createVertex()
//            println("[VERTEX] $i")
            for (j in 0 until attributes.size()) {
                val bmAttr = bmesh.matchAttribute(attributes.get(j))
//                assert(
//                    bmAttr != null &&
//                    bmAttr.numComponents == attributes.get(j).numComponents &&
//                    bmesh.offsets[bmAttr] == attributes.get(j).offset / 4
//                )
                val attribute = attributes.get(j)
                val offset = attribute.offset / 4
                val size = attribute.numComponents
                val data = FloatArray(size)
                for (k in 0 until size) {
                    data[k] = verticesArray[i * stride + offset + k]
                }
                println("Attribute: ${attribute.alias}, data: ${data.joinToString()}")
                loadIntoAttribute(bmAttr!!, vertex, data)
            }
        }

        for (i in 0 until indicesAmount step 3) {
//            println("[FACE] ${indicesArray[i]}, ${indicesArray[i + 1]}, ${indicesArray[i + 2]}")

            val v0 = bmesh.vertexData[indicesArray[i].toInt()]
            val v1 = bmesh.vertexData[indicesArray[i + 1].toInt()]
            val v2 = bmesh.vertexData[indicesArray[i + 2].toInt()]
            bmesh.createFace(v0, v1, v2)
        }


        resultString = "MESH: ${bmesh.vertexData.size} vertices, ${bmesh.faceData.size} faces\n"
        println(resultString)
        writeFile(verticesArray, indicesArray, verticesAmount)
        return bmesh
    }

    fun loadIntoAttribute(attribute: AbstractProperty<BMVertex, *, *>, vertex: BMVertex, data: FloatArray) {
        when (attribute) {
            is FloatTupleAttribute -> {
                attribute[vertex] = data
            }
            is FloatAttribute -> {
                attribute[vertex] = data[0]
            }
            is Vector3Attribute -> {
                attribute[vertex] = Vector3(data[0], data[1], data[2])
            }
            is Vector2Attribute -> {
                attribute[vertex] = Vector2(data[0], data[1])
            }
            is Vector4Attribute -> {
                attribute[vertex] = Vector4(data[0], data[1], data[2], data[3])
            }
        }
    }

    fun writeFile(verts: FloatArray, indices: ShortArray, size: Int = 3) {
        val file = File("mesh.txt")
        file.writeText("Vertices:\n")
        for (i in 0 until verts.size step size) {
            file.appendText("${verts[i]}, ${verts[i + 1]}, ${verts[i + 2]}\n")
        }
        file.appendText("Indices:\n")
        for (i in 0 until indices.size step 3) {
            file.appendText("${indices[i]}, ${indices[i + 1]}, ${indices[i + 2]}\n")
        }
    }

}