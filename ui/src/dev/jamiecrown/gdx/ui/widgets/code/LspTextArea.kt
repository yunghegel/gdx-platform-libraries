//package dev.jamiecrown.gdx.ui.widgets.code
//
//import com.badlogic.gdx.math.Vector2
//import com.badlogic.gdx.scenes.scene2d.Actor
//import com.badlogic.gdx.scenes.scene2d.InputEvent
//import com.badlogic.gdx.scenes.scene2d.ui.Skin
//import com.badlogic.gdx.scenes.scene2d.ui.TextArea
//import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
//import com.badlogic.gdx.utils.Timer
//import dev.jamiecrown.gdx.core.lsp.LspClientUtil
//import org.eclipse.lsp4j.CompletionItem
//import org.eclipse.lsp4j.Hover
//
///**
// * A TextArea that communicates with a Language Server.
// *
// * @param fileUri The URI of the document being edited (e.g., "file:///path/to/shader.glsl").
// * @param languageId The language ID (e.g., "glsl").
// * @param lspClientUtil The utility for communicating with the LSP server.
// * @param skin The UI skin.
// */
//class LspTextArea(
//    private val fileUri: String,
//    private val languageId: String,
//    private val lspClientUtil: LspClientUtil,
//    skin: Skin
//) : TextArea("", skin) {
//
//    // --- Public Callbacks for UI interaction ---
//    var onCompletionRequest: ((List<CompletionItem>, Float, Float) -> Unit)? = null
//    var onHoverRequest: ((Hover, Float, Float) -> Unit)? = null
//    var onHideHoverRequest: (() -> Unit)? = null
//
//    private val hoverTask = object : Timer.Task() {
//        override fun run() {
//            val position = cursorPosition
//            val line = getLineAt(position)
//            val char = position - lines[line].offset
//            val coords = localToStageCoordinates(Vector2(cursorX, cursorY))
//
//            lspClientUtil.requestHover(fileUri, line, char).thenAccept { hover ->
//                if (hover?.contents != null) {
//                    onHoverRequest?.invoke(hover, coords.x, coords.y)
//                }
//            }
//        }
//    }
//
//    init {
//        // Notify the server when the document is first opened
//        lspClientUtil.notifyDidOpen(fileUri, languageId, text)
//
//        setupListeners()
//    }
//
//    private fun setupListeners() {
//        // Listener for text changes
//        setTextFieldListener { _, _ ->
//            lspClientUtil.notifyDidChange(fileUri, text)
//            // Optional: You might want to trigger completion automatically on certain characters
//        }
//
//        // Listener for hover and completion triggers
//        addListener(object : ClickListener() {
//            override fun keyTyped(event: InputEvent?, character: Char): Boolean {
//                // Trigger completion on '.'
//                if (character == '.') {
//                    requestCompletion()
//                }
//                return super.keyTyped(event, character)
//            }
//
//            override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
//                onHideHoverRequest?.invoke() // Hide previous hover
//                hoverTask.cancel() // Cancel any pending hover task
//                Timer.schedule(hoverTask, 0.75f) // Schedule a new hover request
//                return super.mouseMoved(event, x, y)
//            }
//
//            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
//                onHideHoverRequest?.invoke()
//                hoverTask.cancel()
//                super.exit(event, x, y, pointer, toActor)
//            }
//        })
//    }
//
//    private fun requestCompletion() {
//        val position = cursorPosition
//        val line =  getLineAt(position)
//        val char = position - lines[line].offset
//        val coords = localToStageCoordinates(Vector2(cursorX, cursorY + font.lineHeight))
//
//        lspClientUtil.requestCompletion(fileUri, line, char).thenAccept { result ->
//            val items = if (result.isLeft) result.left else result.right.items
//            if (items.isNotEmpty()) {
//                // Invoke the callback to show the completion UI
//                onCompletionRequest?.invoke(items, coords.x, coords.y)
//            }
//        }
//    }
//
//    /**
//     * Inserts a completion string at the current cursor position.
//     */
//    fun insertCompletion(text: String) {
//        // A more robust implementation would replace the partial word
//        // instead of just inserting.
//        insert(cursorPosition, text)
//    }
//}