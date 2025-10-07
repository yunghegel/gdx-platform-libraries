package dev.jamiecrown.gdx.ui.widget.console

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.TextArea
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import dev.jamiecrown.gdx.ui.scene2d.STable

class ConsoleWidget(private val commandProcessor: CommandProcessor = CommandProcessor()) : STable() {

    private val outputArea: TextArea
    private val inputField: TextField

    init {
        // Configure table
        setFillParent(true)
        pad(10f)
        top().left()

        // Output area
        outputArea = TextArea("", skin)
        outputArea.setDisabled(true)

        // Input field
        inputField = TextField("", skin)

        // Layout
        add(outputArea).expand().fill().colspan(2).row()
        add(inputField).expandX().fillX()

        setupInputListener()
    }

    private fun setupInputListener() {
        inputField.addListener(object : ClickListener() {
            override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == Input.Keys.ENTER) {
                    val commandText = inputField.text
                    if (commandText.isNotBlank()) {
                        log(">> $commandText")
                        val result = commandProcessor.execute(commandText)
                        if (result.isNotBlank()) {
                            log(result)
                        }
                        inputField.text = ""
                    }
                    return true
                }
                return false
            }
        })
    }

    fun log(message: String) {
        outputArea.text += message + "\n"
        outputArea.invalidateHierarchy()
        outputArea.layout()
    }
}