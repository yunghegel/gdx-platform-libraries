package dev.jamiecrown.gdx.geometry.data.spatial

import com.badlogic.gdx.math.Vector3
import dev.jamiecrown.gdx.geometry.data.Triangle

data class RaycastHit(
    val triangle: Triangle,
    val point: Vector3,
    val distance: Float
)