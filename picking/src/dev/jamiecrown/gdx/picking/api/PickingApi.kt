package dev.jamiecrown.gdx.picking.api

import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Matrix4

/**
 * Generic picking result wrapper. T can be any domain-specific hit information.
 */
data class PickHit<T>(val payload: T, val x: Int, val y: Int)

/**
 * Abstraction for systems that can perform a pick query at window coordinates.
 */
interface PickDetector<T> {
    fun pickAt(x: Int, y: Int, cameraCombined: Matrix4, pickWidth: Int = 0, pickHeight: Int = 0): T?
}

/**
 * Encodes an application-defined id into RGBA channels for color-based picking.
 */
interface PixelEncoder<Id> {
    fun encode(id: Id): FloatArray // length 4 (r,g,b,a)
}

/**
 * Decodes RGBA channels back into an application-defined hit payload.
 */
interface PixelDecoder<T> {
    fun decode(r: Int, g: Int, b: Int, a: Int): T?
}

/**
 * Something that knows how to render a special picking pass into the given FBO.
 * Implementations usually bind a shader that outputs the encoded color per primitive.
 */
interface PickPassRenderer {
    fun renderPickPass(cameraCombined: Matrix4, targetFbo: FrameBuffer)
}
