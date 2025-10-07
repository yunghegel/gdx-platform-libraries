package dev.jamiecrown.gdx.test

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener
import dev.jamiecrown.gdx.core.util.ThreadHelper
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class LibGdxTestContext internal constructor(
    private val extensionContext: ExtensionContext,
    override val config: LibGdxConfig?
) : BaseTestPrefab(config) {

    val initFunctions = mutableListOf<LibGdxTestContext.() -> Unit>()
    val teardownFunctions = mutableListOf<LibGdxTestContext.() -> Unit>()

    fun joinFunction(first : LibGdxTestContext.() -> Unit, second: LibGdxTestContext.() -> Unit): LibGdxTestContext.() -> Unit {
        return {
            first()
            second()
        }
    }

    operator fun plusAssign(f: LibGdxTestContext.() -> Unit) {
        initFunctions.add(f)
    }



    // DSL fields the test will set:
    var onCreate: (LibGdxTestContext.() -> Unit)? = null
    var onRender: (LibGdxTestContext.() -> Unit)? = null
    var onResize: (LibGdxTestContext.(w: Int, h: Int) -> Unit)? = null
    var onPause: (LibGdxTestContext.() -> Unit)? = null
    var onResume: (LibGdxTestContext.() -> Unit)? = null
    var onDispose: (LibGdxTestContext.() -> Unit)? = null

    // Simple per-test state available to the lambdas:
    var frame: Int = 0


    // Internals:
    @Volatile
    var appInstance: Any? = null // Lwjgl3Application or HeadlessApplication
    @OptIn(ExperimentalAtomicApi::class)
    private val started = AtomicBoolean(false)
    @OptIn(ExperimentalAtomicApi::class)
    private val stopped = AtomicBoolean(false)
    private var latch: CountDownLatch? = null
    @Volatile
    private var inRenderThread: Boolean = false

    /**
     * DSL helpers
     */
    fun create(block: LibGdxTestContext.() -> Unit) { onCreate = initFunctions.fold(block) { acc, f -> joinFunction(acc, f) } }
    fun render(block: LibGdxTestContext.() -> Unit) { onRender = block }
    fun resize(block: LibGdxTestContext.(w: Int, h: Int) -> Unit) { onResize = block }
    fun pause(block: LibGdxTestContext.() -> Unit) { onPause = block }
    fun resume(block: LibGdxTestContext.() -> Unit) { onResume = block }
    fun dispose(block: LibGdxTestContext.() -> Unit) { onDispose = block }


    init {
    }

    /**
     * Start the application in non-blocking mode.
     * If start() has already been called, this is a no-op.
     * If framesOverride != null, that value is used instead of config.frames.
     */
    @OptIn(ExperimentalAtomicApi::class)
    fun start(framesOverride: Int? = null) {
        if (!started.compareAndSet(false, true)) return // already started
        val framesToRun = framesOverride ?: config?.frames ?: 1
        latch = CountDownLatch(1)

        ensureMacOSFirstThreadIfNeeded()

        val listener = object : ApplicationListener {
            override fun create() {
                if (config?.mode != LibGdxConfig.Mode.HEADLESS) {
                    initialize()

                }
                try {
                    onCreate?.invoke(this@LibGdxTestContext)
                } catch (t: Throwable) {
                    // if create fails — stop & propagate to latch
                    stopInternal()
                    throw t
                }
            }

            override fun render() {
                inRenderThread = true
                clear()
                try {
                    onRender?.invoke(this@LibGdxTestContext)
                } catch (t: Throwable) {
                    stopInternal()
                    throw t
                } finally {
                    // increment frame counter after each render call
                    frame += 1
                    if (framesToRun >= 0 && frame >= framesToRun) {
                        stopInternal()
                    }
                    inRenderThread = false
                }
            }

            override fun resize(width: Int, height: Int) {
                onResize?.invoke(this@LibGdxTestContext, width, height)
            }

            override fun pause() {
                onPause?.invoke(this@LibGdxTestContext)
            }

            override fun resume() {
                onResume?.invoke(this@LibGdxTestContext)
            }

            override fun dispose() {
                onDispose?.invoke(this@LibGdxTestContext)
                teardown()
            }
        }

        if (config?.mode == LibGdxConfig.Mode.HEADLESS) {
            val headlessCfg = HeadlessApplicationConfiguration()
            val app = HeadlessApplication(listener, headlessCfg)
            appInstance = app
        } else {
            val cfg = Lwjgl3ApplicationConfiguration().apply {
                setWindowedMode(config?.width ?: 640, config?.height ?: 480)
                setTitle(config?.title ?: "JUnit5 Test")
                setWindowListener(object : Lwjgl3WindowListener {
                    override fun created(window: Lwjgl3Window?) {
                    }

                    override fun iconified(isIconified: Boolean) {
                    }

                    override fun maximized(isMaximized: Boolean) {
                    }

                    override fun focusLost() {
                    }

                    override fun focusGained() {
                    }

                    override fun closeRequested(): Boolean {
                        stopInternal()
                        return true // allow the close
                    }

                    override fun filesDropped(files: Array<out String?>?) {
                    }

                    override fun refreshRequested() {
                    }

                })
                // GL mapping:
//                when (config?.gl ?: LibGdxConfig.GL.GL20) {
//                    LibGdxConfig.GL.GL20 -> { /* default */ }
////                    LibGdxConfig.GL.GL30 ->
////                    LibGdxConfig.GL.GL31 -> useOpenGL3(true, 3, 1)
////                    LibGdxConfig.GL.GL32 -> useOpenGL3(true, 3, 2)
//                }
//                setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL32,3,2)

                if (config?.mode == LibGdxConfig.Mode.HEADLESS) {
                    setWindowedMode(1, 1)
                    setDecorated(false)
                }
            }
            val app = Lwjgl3Application(listener, cfg)
            appInstance = app
        }
    }

    /**
     * Convenience: start and block until the app finishes (frames done or stop() called).
     * Will block forever if frames == -1 and stop() is never called.
     */
    fun run(framesOverride: Int? = null, timeout: Long = 0, unit: TimeUnit = TimeUnit.MILLISECONDS): Boolean {
        start(framesOverride)
        val l = latch!!
        return if (timeout > 0) {
            l.await(timeout, unit)
        } else {
            l.await()
            true
        }
    }

    /** Stop the running application (safe to call from test thread or render thread). */
    fun stop() {
        val callingFromRender = inRenderThread
        stopInternal()
        // If called from the render thread, do not block that thread; otherwise wait until stopped.
        if (!callingFromRender) {
            latch?.await()
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun stopInternal() {
        if (!stopped.compareAndSet(false, true)) return
        try {
            val app = Gdx.app
            if (app != null) {
                if (app is HeadlessApplication || app is Lwjgl3Application) {
                    app.exit() // safe to call from any thread
                } else {
                    throw IllegalStateException("Unsupported Application type: ${app::class.java}")
                }
            }
        } finally {
            latch?.countDown()
        }
    }

    private fun ensureMacOSFirstThreadIfNeeded() {
        ThreadHelper.startNewJvmIfRequired()
    }
}