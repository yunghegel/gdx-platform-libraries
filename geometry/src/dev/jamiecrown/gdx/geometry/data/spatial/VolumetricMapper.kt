package dev.jamiecrown.gdx.geometry.data.spatial

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray
import dev.jamiecrown.gdx.geometry.data.Triangle
import kotlin.math.max
import kotlin.math.min

class VolumetricMapper(
    private val resolution: Vector3 = Vector3(10f, 10f, 10f),
    private val raysPerAxis: Int = 3
) {
    private val tempRay = Ray()
    private val tempIntersection = Vector3()

    /**
     * Maps triangles to the bounding boxes they occupy based on ray intersection tests
     * @param bounds The overall bounds to analyze
     * @param triangles List of triangles to process
     * @param minVoxelSize Minimum size of a voxel (box) in world units
     * @return Map of bounding boxes to lists of triangles contained within them
     */
    fun mapTrianglesToVoxels(
        bounds: BoundingBox,
        triangles: List<Triangle>,
        minVoxelSize: Vector3
    ): List<VoxelMap> {
        val result = mutableListOf<VoxelMap>()

        // Calculate voxel dimensions
        val min = Vector3()
        val max = Vector3()
        bounds.getMin(min)
        bounds.getMax(max)

        val dimensions = Vector3(
            max.x - min.x,
            max.y - min.y,
            max.z - min.z
        )

        // Ensure voxel size is not smaller than minimum
        val voxelSize = Vector3(
            max(dimensions.x / resolution.x, minVoxelSize.x),
            max(dimensions.y / resolution.y, minVoxelSize.y),
            max(dimensions.z / resolution.z, minVoxelSize.z)
        )

        // Calculate number of voxels in each dimension
        val voxelCounts = Vector3(
            max(1f, dimensions.x / voxelSize.x).toInt().toFloat(),
            max(1f, dimensions.y / voxelSize.y).toInt().toFloat(),
            max(1f, dimensions.z / voxelSize.z).toInt().toFloat()
        )

        // Process each voxel
        for (x in 0 until voxelCounts.x.toInt()) {
            for (y in 0 until voxelCounts.y.toInt()) {
                for (z in 0 until voxelCounts.z.toInt()) {
                    val voxelMin = Vector3(
                        min.x + x * voxelSize.x,
                        min.y + y * voxelSize.y,
                        min.z + z * voxelSize.z
                    )
                    val voxelMax = Vector3(
                        min(voxelMin.x + voxelSize.x, max.x),
                        min(voxelMin.y + voxelSize.y, max.y),
                        min(voxelMin.z + voxelSize.z, max.z)
                    )

                    val voxelBounds = BoundingBox(voxelMin, voxelMax)
                    val containedTriangles = findTrianglesInVoxel(voxelBounds, triangles)

                    if (containedTriangles.isNotEmpty()) {
                        result.add(VoxelMap(voxelBounds, containedTriangles.toMutableList()))
                    }
                }
            }
        }

        return result
    }

    private fun findTrianglesInVoxel(
        voxelBounds: BoundingBox,
        triangles: List<Triangle>
    ): List<Triangle> {
        val containedTriangles = mutableSetOf<Triangle>()
        val rayDirections = generateRayDirections()

        // First pass: Quick AABB test
        val potentialTriangles = triangles.filter { triangle ->
            voxelBounds.intersects(triangle.boundingBox)
        }

        if (potentialTriangles.isEmpty()) {
            return emptyList()
        }

        // Second pass: Ray intersection test
        val voxelMin = Vector3()
        val voxelMax = Vector3()
        voxelBounds.getMin(voxelMin)
        voxelBounds.getMax(voxelMax)

        // Cast rays from multiple points along each face of the voxel
        val stepX = (voxelMax.x - voxelMin.x) / (raysPerAxis + 1)
        val stepY = (voxelMax.y - voxelMin.y) / (raysPerAxis + 1)
        val stepZ = (voxelMax.z - voxelMin.z) / (raysPerAxis + 1)

        // Generate ray starting positions on each face
        val rayStarts = mutableListOf<Vector3>()

        // X-axis faces
        for (y in 1..raysPerAxis) {
            for (z in 1..raysPerAxis) {
                rayStarts.add(Vector3(voxelMin.x, voxelMin.y + y * stepY, voxelMin.z + z * stepZ))
                rayStarts.add(Vector3(voxelMax.x, voxelMin.y + y * stepY, voxelMin.z + z * stepZ))
            }
        }

        // Y-axis faces
        for (x in 1..raysPerAxis) {
            for (z in 1..raysPerAxis) {
                rayStarts.add(Vector3(voxelMin.x + x * stepX, voxelMin.y, voxelMin.z + z * stepZ))
                rayStarts.add(Vector3(voxelMin.x + x * stepX, voxelMax.y, voxelMin.z + z * stepZ))
            }
        }

        // Z-axis faces
        for (x in 1..raysPerAxis) {
            for (y in 1..raysPerAxis) {
                rayStarts.add(Vector3(voxelMin.x + x * stepX, voxelMin.y + y * stepY, voxelMin.z))
                rayStarts.add(Vector3(voxelMin.x + x * stepX, voxelMin.y + y * stepY, voxelMax.z))
            }
        }

        // Cast rays and check for intersections
        for (start in rayStarts) {
            for (direction in rayDirections) {
                tempRay.set(start, direction)

                for (triangle in potentialTriangles) {
                    if (triangle.intersectsRay(tempRay, tempIntersection)) {
                        // Verify intersection point is within voxel bounds
                        if (voxelBounds.contains(tempIntersection)) {
                            containedTriangles.add(triangle)
                        }
                    }
                }
            }
        }

        return containedTriangles.toList()
    }

    private fun generateRayDirections(): List<Vector3> {
        // Generate rays in primary and diagonal directions
        return listOf(
            Vector3(1f, 0f, 0f),  // +X
            Vector3(-1f, 0f, 0f), // -X
            Vector3(0f, 1f, 0f),  // +Y
            Vector3(0f, -1f, 0f), // -Y
            Vector3(0f, 0f, 1f),  // +Z
            Vector3(0f, 0f, -1f), // -Z
            Vector3(1f, 1f, 1f).nor(),   // Diagonal
            Vector3(-1f, 1f, 1f).nor(),  // Diagonal
            Vector3(1f, -1f, 1f).nor(),  // Diagonal
            Vector3(1f, 1f, -1f).nor()   // Diagonal
        )
    }
}

data class VoxelMap(
    val bounds: BoundingBox,
    val triangles: List<Triangle>,
)