package dev.jamiecrown.gdx.core.lsp

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

object ExecutableResource {
    fun get(resourcePath: String): File {
        val inputStream: InputStream = this::class.java.getResourceAsStream("/$resourcePath")
            ?: throw IllegalArgumentException("Resource not found in classpath: $resourcePath")


        val tempFile = File.createTempFile("glsl-analyzer-", "")
        tempFile.deleteOnExit()

        FileOutputStream(tempFile).use { outStream ->
            inputStream.use { it.copyTo(outStream) }
        }

        // On macOS/Linux, you must explicitly set the executable bit.
        // This is a safe operation; it will throw an UnsupportedOperationException on
        // Windows, which we can safely ignore as it's not needed there.
        try {
            val permissions = setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
            )
            Files.setPosixFilePermissions(tempFile.toPath(), permissions)
        } catch (e: UnsupportedOperationException) {
            // This platform (e.g., Windows) does not support POSIX permissions.
        }

        return tempFile
    }
}

