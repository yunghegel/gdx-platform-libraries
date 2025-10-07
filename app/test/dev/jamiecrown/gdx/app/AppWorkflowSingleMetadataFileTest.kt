package dev.jamiecrown.gdx.app

import dev.jamiecrown.gdx.state.Injector
import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.storage.FileKeyValueStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AppWorkflowSingleMetadataFileTest {

    @Test
    fun createsOnlyOneMetadataFileWithoutQualifiedName() {
        val dir = File("app/testdata/AppWorkflowSingleMetadataFileTest-" + System.currentTimeMillis()).apply { mkdirs() }
        println("[DEBUG_LOG] Starting AppWorkflowSingleMetadataFileTest.createsOnlyOneMetadataFileWithoutQualifiedName dir=${dir.absolutePath}")

        val store = FileKeyValueStore(dir, "app")
        val state = AppStateManager(store)
        Injector.configure(state)

        val meta = Metadata()
        // Injector no longer auto-registers AppStateNode; we register explicitly like the app does
        state.register(meta)

        // Save to trigger file creation
        state.saveAll()

        val nsDir = File(dir, "app")
        val files = nsDir.listFiles()?.map { it.name }?.sorted().orEmpty()
        println("[DEBUG_LOG] Files created in namespace dir: $files")

        // Expect exactly one file: metadata.properties
        assertEquals(1, files.size, "Expected exactly one file to be created")
        assertTrue(files.contains("metadata.properties"), "Expected metadata.properties to be created, got: $files")
        assertTrue(!files.any { it.contains("dev.jamiecrown.gdx.app.Metadata") }, "Qualified name file should not be created: $files")

        // Mutate and save again to ensure no extra files appear
        meta.version = "9.9.9"
        state.saveAll()
        val filesAfter = nsDir.listFiles()?.map { it.name }?.sorted().orEmpty()
        println("[DEBUG_LOG] Files after second save: $filesAfter")
        assertEquals(files, filesAfter, "No new files should appear after subsequent saves")

        println("[DEBUG_LOG] Completed AppWorkflowSingleMetadataFileTest.createsOnlyOneMetadataFileWithoutQualifiedName; store at ${nsDir.absolutePath}")
    }
}
