package dev.jamiecrown.gdx.test

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import org.junit.jupiter.api.extension.*
import org.slf4j.LoggerFactory
import java.lang.Thread.UncaughtExceptionHandler
import java.lang.reflect.Method
import kotlin.reflect.full.findAnnotation

open class LibGdxTestExtension : ParameterResolver, AfterEachCallback {
    companion object {
        private val NS = ExtensionContext.Namespace.create(LibGdxTestExtension::class.java)
        private const val KEY = "LIBGDX_TEST_CONTEXT"
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type.isAssignableFrom(LibGdxTestContext::class.java)
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val config = findConfig(extensionContext)
        val ctx = LibGdxTestContext(extensionContext, config)
        extensionContext.getStore(NS).put(KEY, ctx)
        return ctx
    }

    override fun afterEach(context: ExtensionContext) {
        val store = context.getStore(NS)
        val ctx = store.remove(KEY, LibGdxTestContext::class.java)
        // Best-effort cleanup: if the app is running, stop and wait a short time
        ctx?.stop()
    }

    private fun findConfig(extensionContext: ExtensionContext): LibGdxConfig? {
        // Prefer method-level annotation; fallback to class-level
        return try {
            val method: Method? = extensionContext.requiredTestMethod
            method?.getAnnotation(LibGdxConfig::class.java)
                ?: extensionContext.requiredTestClass.getAnnotation(LibGdxConfig::class.java)
        } catch (ex: Exception) {
            // If requiredTestMethod isn't present or something else goes wrong, fallback to class annotation
            extensionContext.requiredTestClass.getAnnotation(LibGdxConfig::class.java)
        }
    }
}