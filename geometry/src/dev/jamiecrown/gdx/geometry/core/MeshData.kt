package dev.jamiecrown.gdx.geometry.core

import com.badlogic.gdx.graphics.Mesh
import dev.jamiecrown.gdx.geometry.model.ifs.struct.IEdge
import dev.jamiecrown.gdx.geometry.model.ifs.struct.IFace
import dev.jamiecrown.gdx.geometry.model.ifs.struct.IVertex

abstract class MeshData<V: Vertex, E: Edge, F: Face> {
    abstract fun construct(mesh: Mesh)
}

class IFSMeshData : MeshData<IVertex, IEdge, IFace>() {
    override fun construct(mesh: Mesh) {

    }
}