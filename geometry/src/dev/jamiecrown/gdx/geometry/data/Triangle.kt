package dev.jamiecrown.gdx.geometry.data

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray
import java.lang.Float.max
import java.lang.Float.min

data class Triangle(
    val index: Int,
    val v1: Vector3,
    val v2: Vector3,
    val v3: Vector3,
    val userData: Any? = null
) {
    var boundingBox: BoundingBox = calculateBoundingBox()

    private val normal = Vector3()
    private val edge1 = Vector3()
    private val edge2 = Vector3()

    init {
        updateGeometry()
    }

    fun updateGeometry() {
        calculateBoundingBox()
        edge1.set(v2).sub(v1)
        edge2.set(v3).sub(v1)
        normal.set(edge1).crs(edge2).nor()
    }

    private fun calculateBoundingBox(): BoundingBox {
        val min = Vector3(
            min(min(v1.x, v2.x), v3.x),
            min(min(v1.y, v2.y), v3.y),
            min(min(v1.z, v2.z), v3.z)
        )
        val max = Vector3(
            max(max(v1.x, v2.x), v3.x),
            max(max(v1.y, v2.y), v3.y),
            max(max(v1.z, v2.z), v3.z)
        )
        boundingBox = BoundingBox(min, max)
        return boundingBox
    }

    fun barrycentricCoordinates(point: Vector3): Vector3 {
        val v2v1 = Vector3(v2).sub(v1)
        val v3v1 = Vector3(v3).sub(v1)
        val v2p = Vector3(point).sub(v2)
        val v3p = Vector3(point).sub(v3)

        val d00 = v2v1.dot(v2v1)
        val d01 = v2v1.dot(v3v1)
        val d11 = v3v1.dot(v3v1)
        val d20 = v2p.dot(v2v1)
        val d21 = v3p.dot(v3v1)

        val denom = d00 * d11 - d01 * d01
        val v = (d11 * d20 - d01 * d21) / denom
        val w = (d00 * d21 - d01 * d20) / denom
        val u = 1f - v - w

        return Vector3(u, v, w)
    }

    // Möller–Trumbore intersection algorithm
    fun intersectsRay(ray: Ray, intersection: Vector3? = null): Boolean {
        val pvec = Vector3()
        val tvec = Vector3()
        val qvec = Vector3()

        pvec.set(ray.direction).crs(edge2)
        val det = edge1.dot(pvec)

        // Ray is parallel to triangle
        if (kotlin.math.abs(det) < 1e-6f) return false

        val invDet = 1f / det
        tvec.set(ray.origin).sub(v1)
        val u = tvec.dot(pvec) * invDet

        if (u < 0f || u > 1f) return false

        qvec.set(tvec).crs(edge1)
        val v = ray.direction.dot(qvec) * invDet

        if (v < 0f || u + v > 1f) return false

        val t = edge2.dot(qvec) * invDet

        if (t < 0f) return false

        // Calculate intersection point if requested
        intersection?.set(ray.direction)?.scl(t)?.add(ray.origin)

        if (intersection != null) {
            val barycentric = barrycentricCoordinates(intersection!!)
            intersection.set(
                v1.x * barycentric.x + v2.x * barycentric.y + v3.x * barycentric.z,
                v1.y * barycentric.x + v2.y * barycentric.y + v3.y * barycentric.z,
                v1.z * barycentric.x + v2.z * barycentric.y + v3.z * barycentric.z
            )
        }

        return true
    }


    fun getDistance(camera: Camera): Float {
        val center = Vector3()
        boundingBox.getCenter(center)
        return camera.position.dst(center)
    }

}