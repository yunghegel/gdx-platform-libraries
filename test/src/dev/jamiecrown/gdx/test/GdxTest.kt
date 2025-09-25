package dev.jamiecrown.gdx.test


import org.junit.jupiter.api.TestInstance

/**
 * Base class for all LibGDX tests. Provides a convenient way to run assertions
 * and code on the OpenGL thread.
 *
 * It's important to use TestInstance.Lifecycle.PER_CLASS if your tests rely on
 * class-level setup, but default PER_METHOD is also fine, just ensure your
 * listener management is robust. For now, PER_CLASS is easier to manage a single GdxApp instance.
 */
@GdxTestSuite // Now correctly references the annotation
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class GdxTest {

    internal lateinit var listener: GdxTestExtension.GdxTestApplicationListener

    protected fun runOnGlThread(block: () -> Unit) {
        listener.runOnGlThread(block)
    }
}