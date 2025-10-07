package dev.jamiecrown.gdx.ui.widgets.code

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import org.eclipse.lsp4j.Diagnostic
import kotlin.math.sin

/**
 * An actor that draws diagnostic underlines (e.g., for errors) over an LspTextArea.
 *
 * @param textArea A reference to the text area it overlays.
 * @param skin A skin containing a drawable (e.g., "white") for drawing lines.
 */
//class DiagnosticsLayer(private val textArea: LspTextArea, skin: Skin) : Widget() {
//    private var diagnostics = listOf<Diagnostic>()
//    private val lineDrawable: Drawable = skin.newDrawable("white", Color.RED)
//    private var time = 0f
//
//    fun updateDiagnostics(newDiagnostics: List<Diagnostic>) {
//        this.diagnostics = newDiagnostics
//    }
//
//    override fun draw(batch: Batch, parentAlpha: Float) {
//        super.draw(batch, parentAlpha)
//        time += Gdx.graphics.deltaTime
//
//        for (diagnostic in diagnostics) {
//            val range = diagnostic.range
//            val start = textArea.lines[range.start.line].offset + range.start.character
//            val end = textArea.lines[range.end.line].offset + range.end.character
//
//            drawWavyLine(batch, start, end)
//        }
//    }
//
//    private fun drawWavyLine(batch: Batch, start: Int, end: Int) {
//        val glyphLayout = textArea.style.font.newLayout()
//        val text = textArea.text
//
//        // This is a simplified calculation. A robust version would handle multi-line ranges.
//        if (start >= text.length || end > text.length) return
//
//        val lineIndex = textArea.getLineAt(start)
//        val lineStartPos = textArea.lines[lineIndex].offset
//
//        glyphLayout.setText(textArea.style.font, text.substring(0, start))
//        val startX = glyphLayout.width
//
//        glyphLayout.setText(textArea.style.font, text.substring(0, end))
//        val endX = glyphLayout.width
//
//        val y = height - (textArea.font.lineHeight * (lineIndex + 1)) + 2f
//
//        // Draw the wavy line
//        var x = startX
//        while (x < endX) {
//            val waveY = y + sin((x + time * 100) * 0.5f) * 1.5f
//            lineDrawable.draw(batch, this.x + x, this.y + waveY, 2f, 1f)
//            x += 2
//        }
//    }
//}