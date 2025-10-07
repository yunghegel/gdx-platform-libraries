package dev.jamiecrown.gdx.geometry.data.spatial

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray
import dev.jamiecrown.gdx.geometry.data.Triangle

data class BoundingBoxHit(val box: BoundingBox, val triangles: List<Triangle>) {
    var distance = 0f

    fun computeDistance(ray: Ray): Float {
        val center = Vector3()
        box.getCenter(center)
        distance = ray.origin.dst(center)
        return distance
    }


}

data class RaycastResult(val depth: Int, val hits: List<BoundingBoxHit>)