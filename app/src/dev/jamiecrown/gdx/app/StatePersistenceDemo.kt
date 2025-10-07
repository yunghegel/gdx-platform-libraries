package dev.jamiecrown.gdx.app

import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.storage.FileKeyValueStore
import java.io.File

/**
 * A tiny demonstration that showcases the state persistence functionality
 * using the [Metadata] AppStateNode.
 *
 * You can run main() to see it work on the filesystem, or call [runDemo]
 * from tests to validate behavior.
 */
object StatePersistenceDemo {

    data class DemoResult(
        val beforeSave: Metadata,
        val afterLoad: Metadata,
        val storeDir: File
    )

    /**
     * Runs through a simple scenario:
     * - Create Metadata node with specific values
     * - Persist to a FileKeyValueStore
     * - Mutate the node to different values
     * - Load from store and verify we got the original values back
     */
    fun runDemo(dir: File): DemoResult {
        require(dir.exists() || dir.mkdirs()) { "Failed to create demo directory: ${dir.absolutePath}" }
        println("[DEBUG_LOG] StatePersistenceDemo.runDemo using dir=${dir.absolutePath}")

        val store = FileKeyValueStore(dir, "app")
        val meta = Metadata().apply {
            version = "1.2.3"
            lastProject = "SpaceRangers"
            lastScene = "Level-01"
            recent_projects = linkedMapOf(
                "Alpha" to "2025-01-01T00:00:00Z",
                "Beta" to "2025-01-02T00:00:00Z",
                "Gamma" to "2025-01-03T00:00:00Z"
            )
            askOnRestart = false
            loadOnStartup = true
        }

        val mgr = AppStateManager(store).register(meta)
        println("[DEBUG_LOG] Saving metadata: $meta")
        mgr.saveAll()

        // Mutate to ensure reload populates from disk values
        meta.version = "0.0.0"
        meta.lastProject = "none"
        meta.lastScene = "none"
        meta.recent_projects.clear()
        meta.askOnRestart = true
        meta.loadOnStartup = false
        println("[DEBUG_LOG] After mutation (pre-load): $meta")

        mgr.loadAll()
        println("[DEBUG_LOG] After load: $meta")

        return DemoResult(
            beforeSave = Metadata().apply {
                version = "1.2.3"
                lastProject = "SpaceRangers"
                lastScene = "Level-01"
                recent_projects = linkedMapOf(
                    "Alpha" to "2025-01-01T00:00:00Z",
                    "Beta" to "2025-01-02T00:00:00Z",
                    "Gamma" to "2025-01-03T00:00:00Z"
                )
                askOnRestart = false
                loadOnStartup = true
            },
            afterLoad = meta,
            storeDir = dir
        )
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val dir = File("app/demo-data/StatePersistenceDemo").apply { mkdirs() }
        val result = runDemo(dir)
        println("[DEBUG_LOG] Demo finished. Data stored at: ${result.storeDir.absolutePath}")
        println("[DEBUG_LOG] Expected (beforeSave): ${result.beforeSave}")
        println("[DEBUG_LOG] Actual (afterLoad):   ${result.afterLoad}")
    }
}
