package dev.jamiecrown.gdx.geometry.data.spatial

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray
import dev.jamiecrown.gdx.geometry.data.Triangle

open class Octree(
    val bounds: BoundingBox,
    internal val maxDepth: Int = 8,
    internal val maxTrianglesPerNode: Int = 128,
    val currentDepth: Int = 0
) {
    var children: Array<Octree>? = null
    val triangles = mutableListOf<Triangle>()
    var dbLines = mutableListOf<Pair<Vector3, Vector3>>()
    var hasSubdivided = false
    var distance = 0f

    fun insert(triangle: Triangle) {
        if (!bounds.intersects(triangle.boundingBox)) {
            return
        }

        if (!hasSubdivided && (currentDepth < maxDepth) && triangles.size >= maxTrianglesPerNode) {
            subdivide()
        }

        if (hasSubdivided) {
            children?.forEach { child -> child.insert(triangle) }
        } else {
            triangles.add(triangle)
        }
    }


    fun query(queryBox: BoundingBox): List<Triangle> {
        if (!bounds.intersects(queryBox)) {
            return emptyList()
        }

        val result = mutableListOf<Triangle>()

        if (hasSubdivided) {
            children?.forEach { child ->
                result.addAll(child.query(queryBox))
            }
        } else {
            triangles.forEach { triangle ->
                if (triangle.boundingBox.intersects(queryBox)) {
                    result.add(triangle)
                }
            }
        }

        return result
    }

    fun raycast(ray: Ray, maxDistance: Float = Float.POSITIVE_INFINITY): List<RaycastHit> {
        if (!intersectRayBox(ray, bounds, maxDistance)) {
            return emptyList()
        }

        val hits = mutableListOf<RaycastHit>()
        val intersection = Vector3()

        if (hasSubdivided) {
            // Sort children by distance from ray origin
            val sortedChildren = children?.sortedBy { child ->
                val center = Vector3()
                child.bounds.getCenter(center)
                ray.origin.dst2(center)
            }

            sortedChildren?.forEach { child ->
                hits.addAll(child.raycast(ray, maxDistance))
            }
        } else {
            triangles.forEach { triangle ->
                if (triangle.intersectsRay(ray, intersection)) {
                    val distance = ray.origin.dst(intersection)
                    if (distance <= maxDistance) {
                        hits.add(RaycastHit(triangle, Vector3(intersection), distance))
                    }
                }
            }
        }

        return hits.sortedBy { it.distance }
    }

    internal fun intersectRayBox(ray: Ray, box: BoundingBox, maxDistance: Float): Boolean {
        val min = Vector3()
        val max = Vector3()
        box.getMin(min)
        box.getMax(max)

        var tmin = (min.x - ray.origin.x) / ray.direction.x
        var tmax = (max.x - ray.origin.x) / ray.direction.x

        if (tmin > tmax) tmin = tmax.also { tmax = tmin }

        var tymin = (min.y - ray.origin.y) / ray.direction.y
        var tymax = (max.y - ray.origin.y) / ray.direction.y

        if (tymin > tymax) tymin = tymax.also { tymax = tymin }

        if (tmin > tymax || tymin > tmax) return false

        if (tymin > tmin) tmin = tymin
        if (tymax < tmax) tmax = tymax

        var tzmin = (min.z - ray.origin.z) / ray.direction.z
        var tzmax = (max.z - ray.origin.z) / ray.direction.z

        if (tzmin > tzmax) tzmin = tzmax.also { tzmax = tzmin }

        if (tmin > tzmax || tzmin > tmax) return false

        if (tzmin > tmin) tmin = tzmin
        if (tzmax < tmax) tmax = tzmax

        return tmin < maxDistance && tmax > 0
    }

    fun queryRay(ray: Ray, maxDistance: Float = Float.POSITIVE_INFINITY): List<BoundingBoxHit> {
        if (!intersectRayBox(ray, bounds, maxDistance)) {
            return emptyList()
        }

        val hits = mutableListOf<BoundingBoxHit>()

        Vector3()
        //data class BoundingBoxHit(val box : BoundingBox, val triangles: List<Triangle>)
        if (hasSubdivided) {
            // Sort children by distance from ray origin
            val sortedChildren = children?.sortedBy { child ->
                val center = Vector3()
                child.bounds.getCenter(center)
                val dist = ray.origin.dst2(center)
                child.distance = dist
                dist
            }

            sortedChildren?.forEach { child ->
                hits.addAll(child.queryRay(ray, maxDistance))
            }
        } else {
            hits.add(BoundingBoxHit(bounds, triangles))
        }
        return hits
    }

    private fun subdivide() {
        val center = Vector3()
        bounds.getCenter(center)

        val min = Vector3()
        bounds.getMin(min)
        val max = Vector3()
        bounds.getMax(max)

        children = Array(8) { index ->
            val childMin = Vector3()
            val childMax = Vector3()

            // Calculate bounds for each octant
            childMin.x = if (index and 1 == 0) min.x else center.x
            childMin.y = if (index and 2 == 0) min.y else center.y
            childMin.z = if (index and 4 == 0) min.z else center.z

            childMax.x = if (index and 1 == 0) center.x else max.x
            childMax.y = if (index and 2 == 0) center.y else max.y
            childMax.z = if (index and 4 == 0) center.z else max.z

            Octree(
                BoundingBox(childMin, childMax),
                maxDepth,
                maxTrianglesPerNode,
                currentDepth + 1
            )
        }

        // Redistribute existing triangles to children
        triangles.forEach { triangle ->
            children?.forEach { child -> child.insert(triangle) }
        }
        triangles.clear()
        hasSubdivided = true
        dbLines.addAll(getDebugLines())
    }

    // Helper method to visualize the octree structure (useful for debugging)
    fun getDebugLines(): List<Pair<Vector3, Vector3>> {
        val lines = mutableListOf<Pair<Vector3, Vector3>>()

        val min = Vector3()
        bounds.getMin(min)
        val max = Vector3()
        bounds.getMax(max)

        // Add lines for current node
        lines.add(Pair(Vector3(min.x, min.y, min.z), Vector3(max.x, min.y, min.z)))
        lines.add(Pair(Vector3(min.x, min.y, min.z), Vector3(min.x, max.y, min.z)))
        lines.add(Pair(Vector3(min.x, min.y, min.z), Vector3(min.x, min.y, max.z)))
        lines.add(Pair(Vector3(max.x, max.y, max.z), Vector3(min.x, max.y, max.z)))
        lines.add(Pair(Vector3(max.x, max.y, max.z), Vector3(max.x, min.y, max.z)))
        lines.add(Pair(Vector3(max.x, max.y, max.z), Vector3(max.x, max.y, min.z)))

        // Add lines from children
        if (hasSubdivided) {
            children?.forEach { child ->
                lines.addAll(child.getDebugLines())
            }
        }

        return lines
    }

    fun getVolumetricMap(
        resolution: Vector3 = Vector3(10f, 10f, 10f),
        raysPerAxis: Int = 3,
        minVoxelSize: Vector3 = Vector3(0.1f, 0.1f, 0.1f)
    ): List<VoxelMap> {
        val mapper = VolumetricMapper(resolution, raysPerAxis)
        val allTriangles = mutableListOf<Triangle>()

        // Helper function to collect all triangles
        fun collectTriangles(node: Octree) {
            if (node.hasSubdivided) {
                node.children?.forEach { collectTriangles(it) }
            } else {
                allTriangles.addAll(node.triangles)
            }
        }

        collectTriangles(this)
        return mapper.mapTrianglesToVoxels(this.bounds, allTriangles, minVoxelSize)
    }

    fun clear() {
        triangles.clear()
        children?.forEach { it.clear() }
        children = null
        hasSubdivided = false
    }

    override fun toString(): String {
        return buildString {
            append("Octree(depth=$currentDepth, triangles=${triangles.size})\n")
            children?.forEach { child ->
                if (child.triangles.isNotEmpty()) {
                    append("  ".repeat(child.currentDepth + 1))
                    append(child.toString())
                }
            }
        }

    }
}