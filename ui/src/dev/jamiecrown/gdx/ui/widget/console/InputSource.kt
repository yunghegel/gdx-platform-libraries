package dev.jamiecrown.gdx.ui.widget.console

interface InputSource {
    fun readLine() : String?

    fun acceptInput(handler : (String) -> Unit) {
        val line = readLine() ?: return
        handler(line)
    }
}