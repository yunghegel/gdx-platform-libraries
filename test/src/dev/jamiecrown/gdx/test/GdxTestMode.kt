package dev.jamiecrown.gdx.test

import org.junit.jupiter.api.extension.ExtendWith
import java.lang.annotation.Inherited

/**
 * Test mode for LibGDX tests.
 * @property HEADLESS Run the tests in headless mode
 * @property WINDOWED Run the tests in windowed mode with a resolution of 800x600
 */

enum class GdxTestMode {
    HEADLESS,
    WINDOWED
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class GdxConfig(
    val mode: GdxTestMode = GdxTestMode.HEADLESS,
    val width: Int = 800,
    val height: Int = 600,
    val title: String = "GdxTest",
    val useGL30: Boolean = false
)

/**
 * Meta-annotation to mark a test class as a LibGDX test suite and extend it with our custom runner.
 * RENAMED from @GdxTest to avoid conflict with the GdxTest class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@ExtendWith(GdxTestExtension::class) // This still points to our extension
annotation class GdxTestSuite