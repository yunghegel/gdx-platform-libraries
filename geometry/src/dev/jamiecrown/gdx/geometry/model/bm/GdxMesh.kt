package dev.jamiecrown.gdx.geometry.model.bm

import com.badlogic.gdx.graphics.Mesh

object GdxMesh {

    fun convertFrom(bMesh: BMesh) : Mesh {
        bMesh.vertexSize
        val mesh = Mesh(true, bMesh.vertices.count(), bMesh.faces.count(), bMesh.vertexAttributes)
        mesh.setVertices(bMesh.toFloatArray())
        mesh.setIndices(bMesh.toShortArray())

        return mesh
    }

}