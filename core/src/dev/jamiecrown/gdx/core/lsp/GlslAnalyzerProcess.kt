package dev.jamiecrown.gdx.core.lsp

import java.io.File
import java.io.InputStream
import java.io.OutputStream

class GlslAnalyzerProcess {
    private var process: Process? = null

    fun start() {
        val command = listOf(getPath())
        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
        process = processBuilder.start()
    }

    fun getInputStream(): InputStream? {
        return process?.inputStream
    }

    fun getOutputStream(): OutputStream? {
        return process?.outputStream
    }

    fun stop() {
        process?.destroy()
    }

    fun getPath() : String {
//        binary is located at the top level of the resources source directory
        val resource = this::class.java.classLoader.getResource("glsl_analyzer")
        val file = File(resource.toURI())
        return file.absolutePath
    }

}