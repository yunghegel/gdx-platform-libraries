package dev.jamiecrown.gdx.core.util


const val EPSILON = 1e-6

fun floatEquals(a: Float, b: Float): Boolean {
    return a == b || kotlin.math.abs(a - b) <= EPSILON
}

fun floatEquals(a: Float, b: Float, epsilon: Float): Boolean {
    return a == b || kotlin.math.abs(a - b) <= epsilon
}

fun doubleEquals(a: Double, b: Double): Boolean {
    return a == b || kotlin.math.abs(a - b) <= EPSILON
}

fun doubleEquals(a: Double, b: Double, epsilon: Double): Boolean {
    return a == b || kotlin.math.abs(a - b) <= epsilon
}

