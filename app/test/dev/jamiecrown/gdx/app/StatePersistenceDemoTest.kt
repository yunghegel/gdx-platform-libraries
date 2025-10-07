package dev.jamiecrown.gdx.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class StatePersistenceDemoTest {

    @Test
    fun roundTripMetadata() {
        val dir = File("app/testdata/StatePersistenceDemoTest-" + System.currentTimeMillis()).apply { mkdirs() }
        println("[DEBUG_LOG] Starting StatePersistenceDemoTest.roundTripMetadata with dir=${dir.absolutePath}")
        val result = StatePersistenceDemo.runDemo(dir)

        // Validate expected values survived reload
        assertEquals(result.beforeSave.version, result.afterLoad.version)
        assertEquals(result.beforeSave.lastProject, result.afterLoad.lastProject)
        assertEquals(result.beforeSave.lastScene, result.afterLoad.lastScene)
        assertEquals(result.beforeSave.recent_projects, result.afterLoad.recent_projects)
        assertEquals(result.beforeSave.askOnRestart, result.afterLoad.askOnRestart)
        assertEquals(result.beforeSave.loadOnStartup, result.afterLoad.loadOnStartup)
        println("[DEBUG_LOG] Completed StatePersistenceDemoTest.roundTripMetadata; store at ${result.storeDir}")
    }
}
