package dev.jamiecrown.gdx.test

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LibGdxConfig(
    val mode: Mode = Mode.WINDOWED,
    val width: Int = 640,
    val height: Int = 480,
    val title: String = "JUnit5 Test",
    val gl: GL = GL.GL20,
    val frames: Int = 1,
    val prefab: KClass<out TestPrefab> = BaseTestPrefab::class,
    val modelPath: String = ""
) {
    enum class Mode { HEADLESS, WINDOWED }
    enum class GL { GL20, GL30, GL31, GL32 }
}
