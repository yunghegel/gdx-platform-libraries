package dev.jamiecrown.gdx.app

import com.sun.tools.sjavac.options.Options.parseArgs
import dev.jamiecrown.gdx.state.Injector
import dev.jamiecrown.gdx.state.app.AppStateManager
import dev.jamiecrown.gdx.state.provision
import dev.jamiecrown.gdx.state.storage.FileKeyValueStore
import java.io.File

object App {


    fun parseArgs(args: Array<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            when {
                arg.startsWith("--") -> {
                    val key = arg.substring(2)
                    if (i + 1 < args.size && !args[i + 1].startsWith("-")) {
                        map[key] = args[i + 1]
                        i++
                    } else {
                        map[key] = "true"
                    }
                }

                arg.startsWith("-") -> {
                    val key = arg.substring(1)
                    if (i + 1 < args.size && !args[i + 1].startsWith("-")) {
                        map[key] = args[i + 1]
                        i++
                    } else {
                        map[key] = "true"
                    }
                }

                arg.contains("=") -> {
                    val (key, value) = arg.split("=", limit = 2)
                    map[key] = value
                }

                else -> {
                    map[arg] = "true"
                }
            }
            i++
        }
        return map
    }


    @JvmStatic
    fun main(args: Array<String>) {
        val arguments = parseArgs(args)
        val rootDir = arguments["rootDirectory"] ?: (System.getProperty("user.home") + File.separator + ".salient")

        val project = arguments["project"] ?: "default|none"

        val scene = arguments["scene"] ?: "default|none"

        val kv = FileKeyValueStore(File(rootDir), "salient")

        val state: AppStateManager by provision(kv)

        Injector.configure(state)

        val meta: Metadata by provision()
        meta.recent_projects.putAll(mapOf("today" to java.time.Instant.now().toString()))
        state.register(meta)


    }
}