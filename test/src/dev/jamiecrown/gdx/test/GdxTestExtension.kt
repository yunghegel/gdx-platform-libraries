package dev.jamiecrown.gdx.test

import com.badlogic.gdx.Application
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import org.junit.jupiter.api.extension.*
import org.junit.platform.commons.support.AnnotationSupport
import org.lwjgl.opengl.GL20
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * A JUnit 5 Extension that manages the LibGDX application lifecycle for tests.
 */
class GdxTestExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private var application: Application? = null
    private val testListener = GdxTestApplicationListener()

    // --- BeforeAllCallback ---
    override fun beforeAll(context: ExtensionContext?) {
        // Find GdxConfig at class level
        val gdxConfig = AnnotationSupport.findAnnotation(context?.requiredTestClass, GdxConfig::class.java).orElse(null)
            ?: AnnotationSupport.findAnnotation(context?.element, GdxConfig::class.java).orElse(GdxConfig())

        println("Starting LibGDX application for test class: ${context?.requiredTestClass?.simpleName} in ${gdxConfig.mode} mode.")

        // Start the LibGDX application in a separate thread.
        // We use a latch to wait for the application to be initialized.
        val initLatch = CountDownLatch(1)
        val appThread = Thread {
            application = when (gdxConfig.mode) {
                GdxTestMode.HEADLESS -> {
                    val config = HeadlessApplicationConfiguration()
                    // Configure headless if needed
                    HeadlessApplication(testListener, config).also { initLatch.countDown() }
                }
                GdxTestMode.WINDOWED -> {
                    val config = Lwjgl3ApplicationConfiguration().apply {
                        setWindowedMode(gdxConfig.width, gdxConfig.height)
                        setTitle(gdxConfig.title)
                        useVsync(false) // Don't block for Vsync in tests
                        // Other configurations like useGL30, etc.
                    }
                    Lwjgl3Application(testListener, config).also { initLatch.countDown() }
                }
            }
        }
        appThread.name = "GdxAppThread-${context?.requiredTestClass?.simpleName}"
        appThread.start()

        // Wait for the application to start and Gdx to be initialized.
        if (!initLatch.await(10, TimeUnit.SECONDS)) {
            throw IllegalStateException("LibGDX application did not initialize within 10 seconds.")
        }
    }

    // --- AfterAllCallback ---
    override fun afterAll(context: ExtensionContext?) {
        println("Stopping LibGDX application for test class: ${context?.requiredTestClass?.simpleName}")
        application?.exit()
        application = null // Clear reference
    }

    // --- BeforeEachCallback ---
    override fun beforeEach(context: ExtensionContext?) {
        // ...
        testListener.setTestRunnable { // This runnable is executed on the GL thread
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f) // This IS on the GL thread, so it should be fine.
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }
        testListener.executeTestRunnable() // This POSTS the runnable to the GL thread
        // ...
    }

    // --- AfterEachCallback ---
    override fun afterEach(context: ExtensionContext?) {
        // Clear any specific per-test state in the listener
        testListener.setTestRunnable(null)
    }

    /**
     * This is the heart of the testing framework. It acts as the ApplicationListener
     * and runs the actual test methods on the LibGDX application thread.
     */
    inner class GdxTestApplicationListener : ApplicationListener {

        private val testMethodLatch = AtomicReference<CountDownLatch?>(null)
        private val testResult = AtomicReference<Throwable?>(null)
        private val methodToRun = AtomicReference<() -> Unit>(null)
        private var isInitialized = false

        // A runnable that can be set by the test to execute on the GL thread
        private var glThreadRunnable: (() -> Unit)? = null

        fun setTestRunnable(runnable: (() -> Unit)?) {
            glThreadRunnable = runnable
        }

        fun executeTestRunnable() {
            // This method is called from the JUnit test thread,
            // we need to signal the GL thread to execute the runnable.
            if (glThreadRunnable != null) {
                Gdx.app.postRunnable { glThreadRunnable?.invoke() }
            }
        }

        override fun create() {
            isInitialized = true
            println("LibGDX application listener created.")
        }

        override fun resize(width: Int, height: Int) {
            println("LibGDX application resized to $width x $height.")
        }

        override fun render() {
            // The render loop is where we can execute test methods.
            // When a test method is scheduled, it will be executed here.
            val currentMethod = methodToRun.getAndSet(null)
            if (currentMethod != null) {
                try {
                    currentMethod.invoke()
                } catch (t: Throwable) {
                    testResult.set(t)
                } finally {
                    testMethodLatch.getAndSet(null)?.countDown()
                }
            }
        }

        override fun pause() {
            println("LibGDX application paused.")
        }

        override fun resume() {
            println("LibGDX application resumed.")
        }

        override fun dispose() {
            println("LibGDX application listener disposed.")
            isInitialized = false
        }

        /**
         * Schedules a test method to be run on the LibGDX GL thread.
         * The JUnit test thread will block until the method completes or throws an exception.
         */
        fun runOnGlThread(testBlock: () -> Unit) {
            val latch = CountDownLatch(1)
            testMethodLatch.set(latch)
            methodToRun.set(testBlock)

            // Post a runnable to ensure the render method is called soon to pick up the test.
            // Or just rely on the render loop if it's running fast enough.
            // Gdx.app.postRunnable { /* signal render loop */ } // Not strictly necessary if render is frequent.

            // Wait for the test method to execute and signal completion.
            if (!latch.await(20, TimeUnit.SECONDS)) { // Increased timeout for potentially complex tests
                throw IllegalStateException("Test method did not execute within 20 seconds on GL thread.")
            }

            testResult.getAndSet(null)?.let { throw it } // Re-throw any exception from the GL thread
        }
    }
}