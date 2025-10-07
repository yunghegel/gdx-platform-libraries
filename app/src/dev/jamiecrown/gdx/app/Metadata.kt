package dev.jamiecrown.gdx.app

import dev.jamiecrown.gdx.state.Persist
import dev.jamiecrown.gdx.state.app.AppStateNode

class Metadata : AppStateNode{

    override val id: String = "metadata"

    override val dependsOn: Set<String> = emptySet()

    /*
    Application version.
     */
    @Persist("version")
    var version: String = "0.0.1"

    /*
    Last opened project.
     */
    @Persist("last_project")
    var lastProject : String = "none"

    /*
    Last opened scene.
     */
    @Persist("last_scene")
    var lastScene : String = "none"

    /*
    Recent projects map (projectName -> lastOpenedIsoTime).
    Using a Map here because the reflective persistence currently supports Map types.
     */
    @Persist("recent_projects")
    var recent_projects : MutableMap<String, String> = linkedMapOf()

    /*
    Whether to ask the user to save changes on restart.
     */
    @Persist("ask_on_restart")
    var askOnRestart : Boolean = true

    /*
     Whether to load the last project and scene on startup.
     */
    @Persist("load_on_startup")
    var loadOnStartup : Boolean = true

    override fun toString(): String {
        return "Metadata(version='$version', lastProject='$lastProject', lastScene='$lastScene', recent_projects=$recent_projects, askOnRestart=$askOnRestart, loadOnStartup=$loadOnStartup)"
    }

}