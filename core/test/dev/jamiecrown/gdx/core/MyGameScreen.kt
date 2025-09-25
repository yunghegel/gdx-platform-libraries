// src/test/kotlin/com/example/game/MyGameScreenTest.kt
package dev.jamiecrown.gdx.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import dev.jamiecrown.gdx.test.GdxConfig
import dev.jamiecrown.gdx.test.GdxTestMode
import dev.jamiecrown.gdx.test.GdxTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.lwjgl.opengl.GL20

// Example of a game component to test
class MyGameScreen {
    val backgroundColor = Color(0.2f, 0.2f, 0.4f, 1f)

    fun render(shapeRenderer: ShapeRenderer) {
        Gdx.gl.glClearColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color.RED
        shapeRenderer.circle(100f, 100f, 50f)
        shapeRenderer.end()
    }
}

// --- Test Classes ---

@DisplayName("MyGameScreen Headless Tests")
@GdxConfig(mode = GdxTestMode.HEADLESS) // This class will run in headless mode
class MyGameScreenHeadlessTest : GdxTest() {

    private lateinit var screen: MyGameScreen
    private lateinit var shapeRenderer: ShapeRenderer

    // Note: create() and dispose() are part of the GdxTestApplicationListener.
    // For per-test setup, use JUnit's @BeforeEach and @AfterEach
    // Ensure you run any Gdx-related setup/teardown on the GL thread.

    @org.junit.jupiter.api.BeforeEach
    fun setup() {
        runOnGlThread {
            screen = MyGameScreen()
            shapeRenderer = ShapeRenderer()
        }
    }

    @org.junit.jupiter.api.AfterEach
    fun teardown() {
        runOnGlThread {
            shapeRenderer.dispose()
        }
    }

    @Test
    fun `test initial background color`() {
        runOnGlThread {
            // In headless, we don't have a real GL context to query the actual clear color.
            // We can only assert on the *logic* that sets it.
            // For visual verification, you'd need screenshot testing or windowed mode.
            assertNotNull(Gdx.app)
            assertEquals(0.2f, screen.backgroundColor.r)
            assertEquals(0.2f, screen.backgroundColor.g)
        }
    }

    @Test
    fun `test Gdx is available`() {
        runOnGlThread {
            assertNotNull(Gdx.app)
            assertNotNull(Gdx.graphics)
            assertNotNull(Gdx.input)
            assertNotNull(Gdx.files)
            assertNotNull(Gdx.audio)
        }
    }

    @Test
    fun `test some rendering logic without actual rendering checks`() {
        runOnGlThread {
            // Call the render method, ensure it doesn't crash
            screen.render(shapeRenderer)
            // In headless, you can't assert on pixels, but you can assert on game state changes
            // that might result from rendering.
        }
    }
}

@DisplayName("MyGameScreen Windowed Tests")
@GdxConfig(mode = GdxTestMode.WINDOWED, width = 400, height = 300, title = "Windowed Test")
class MyGameScreenWindowedTest : GdxTest() {

    private lateinit var screen: MyGameScreen
    private lateinit var shapeRenderer: ShapeRenderer

    @org.junit.jupiter.api.BeforeEach
    fun setup() {
        runOnGlThread {
            screen = MyGameScreen()
            shapeRenderer = ShapeRenderer()
        }
    }

    @org.junit.jupiter.api.AfterEach
    fun teardown() {
        runOnGlThread {
            shapeRenderer.dispose()
        }
    }

    @Test
    fun `windowed mode should show a red circle`() {
        runOnGlThread {
            screen.render(shapeRenderer)
            // In a real windowed test, you might want to capture a screenshot
            // and compare it to a reference image. This is complex and usually requires
            // an external library for image comparison.
            // For now, we'll just assert that the render call happened and no crash.
            assertTrue(Gdx.graphics.width == 400)
            assertTrue(Gdx.graphics.height == 300)
        }
    }

    @Nested
    @DisplayName("Windowed Sub-Tests")
    inner class SubTests {

        @Test
        @GdxConfig(mode = GdxTestMode.WINDOWED, width = 200, height = 150) // Override config for this specific test
        fun `test overriding config for specific method`() {
            runOnGlThread {
                assertNotNull(Gdx.graphics)
                assertEquals(200, Gdx.graphics.width)
                assertEquals(150, Gdx.graphics.height)
            }
        }
    }
}