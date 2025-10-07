package dev.jamiecrown.gdx.geometry.data.depup

import com.badlogic.gdx.math.Vector3
import dev.jamiecrown.gdx.geometry.core.Vertex
import dev.jamiecrown.gdx.geometry.data.attribute.Vector3Attribute

interface VertexDeduplication {

    fun <V:Vertex> addExisting(vertex: V)

    fun clear()

    fun <V:Vertex> getVertex(location: Vector3) : V

    fun <V:Vertex> getOrCreateVertex(location: Vector3, data: Vector3Attribute<V>) : Vertex

}