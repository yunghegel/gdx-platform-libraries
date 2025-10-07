package dev.jamiecrown.gdx.app

class ProjectManager {

    fun createNewProject(name: String): Project {
        val id = generateProjectId(name)
        return Project(name, id)
    }

    private fun generateProjectId(name: String): String {
        return name.lowercase().replace(" ", "_") + "_" + System.currentTimeMillis()
    }

}