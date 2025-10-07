package dev.jamiecrown.gdx.ui.layout

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import dev.jamiecrown.gdx.ui.UI
import dev.jamiecrown.gdx.ui.scene2d.SMultiSplitPane
import dev.jamiecrown.gdx.ui.widget.DockableWindowActor

/**
 * A layout class that extends SMultiSplitPane to provide a unified API for dockable panels.
 * This class manages multiple DockableWindowActor instances and provides drag-and-drop functionality.
 */
class DockablePanelLayout(vertical: Boolean = false) : SMultiSplitPane(vertical) {

    // Store references to the dockable window actors
    private val dockableActors = mutableListOf<DockableWindowActor>()

    // Drag and drop handler
    private val dragAndDrop = DragAndDrop()

    // Keep track of drag sources and targets for removal
    private val dragSources = mutableMapOf<VisWindow, Source>()
    private val dragTargets = mutableMapOf<VisWindow, Target>()

    // Highlight drawable for drag targets
    private val highlightDrawable: Drawable by lazy { UI.drawable("border-light") }

    init {
        // Initialize drag and drop
        setupDragAndDrop()
    }

    /**
     * Adds a dockable window to the layout.
     * 
     * @param window The window to add
     * @param orientation The orientation of the window (LEFT, RIGHT, TOP, BOTTOM)
     * @param index The index at which to add the window (default: at the end)
     * @param splitRatio The split ratio to use (default: 0.25f for LEFT/TOP, 0.75f for RIGHT/BOTTOM)
     * @return The created DockableWindowActor
     */
    fun addDockableWindow(
        window: VisWindow,
        orientation: PanelOrientation,
        index: Int = dockableActors.size,
        splitRatio: Float = when(orientation) {
            PanelOrientation.LEFT, PanelOrientation.TOP -> 0.25f
            PanelOrientation.RIGHT, PanelOrientation.BOTTOM -> 0.75f
        }
    ): DockableWindowActor {
        // Create a dockable window actor
        val dockableActor = DockableWindowActor(window, orientation)

        // Set the parent layout and index
        dockableActor.setParentLayout(this, index)

        // Add the actor to our list
        dockableActors.add(dockableActor)

        // Add the actor to the split pane
        if (children.size == 0) {
            // First actor, just add it
            setWidgets(dockableActor)
        } else {
            // Add at the specified index
            val currentActors = children.toList().toTypedArray()
            val newActors = arrayOfNulls<Actor>(currentActors.size + 1)

            // Copy actors before the index
            for (i in 0 until index) {
                newActors[i] = currentActors[i]
            }

            // Add the new actor
            newActors[index] = dockableActor

            // Copy actors after the index
            for (i in index until currentActors.size) {
                newActors[i + 1] = currentActors[i]
            }

            // Set the widgets
            setWidgets(*newActors.filterNotNull().toTypedArray())

            // Set the split ratio
            if (index > 0) {
                setSplit(index - 1, splitRatio)
            }
        }

        // Add the window to the drag and drop system
        addWindowToDragAndDrop(window)

        return dockableActor
    }

    /**
     * Removes a dockable window from the layout.
     * 
     * @param window The window to remove
     * @return True if the window was removed, false otherwise
     */
    fun removeDockableWindow(window: VisWindow): Boolean {
        val actor = dockableActors.find { it.window == window } ?: return false

        // Remove the actor from our list
        dockableActors.remove(actor)

        // Remove the actor from the split pane
        removeActor(actor)

        // Remove the window from the drag and drop system
        dragSources[window]?.let { source ->
            dragAndDrop.removeSource(source)
            dragSources.remove(window)
        }

        dragTargets[window]?.let { target ->
            dragAndDrop.removeTarget(target)
            dragTargets.remove(window)
        }

        return true
    }

    /**
     * Collapses a dockable window.
     * 
     * @param window The window to collapse
     * @return True if the window was collapsed, false otherwise
     */
    fun collapseWindow(window: VisWindow): Boolean {
        val actor = dockableActors.find { it.window == window } ?: return false
        actor.collapse()
        return true
    }

    /**
     * Expands a dockable window.
     * 
     * @param window The window to expand
     * @return True if the window was expanded, false otherwise
     */
    fun expandWindow(window: VisWindow): Boolean {
        val actor = dockableActors.find { it.window == window } ?: return false
        actor.expand()
        return true
    }

    /**
     * Toggles the collapse state of a dockable window.
     * 
     * @param window The window to toggle
     * @return True if the window was toggled, false otherwise
     */
    fun toggleWindowCollapse(window: VisWindow): Boolean {
        val actor = dockableActors.find { it.window == window } ?: return false
        actor.toggleCollapse()
        return true
    }

    /**
     * Sets up drag and drop functionality for the layout.
     */
    private fun setupDragAndDrop() {
        // Configure drag and drop
        dragAndDrop.setDragActorPosition(-16f, 16f) // Offset the drag actor
    }

    /**
     * Adds a window to the drag and drop system.
     * 
     * @param window The window to add
     */
    private fun addWindowToDragAndDrop(window: VisWindow) {
        val titleTable = window.titleTable
        val previousBackground = window.background
        val dragColor = Color(Color.LIGHT_GRAY).apply { a = 0.7f }

        // Create source (dragging from the title table)
        val source = object : Source(titleTable) {
            val payload = Payload()

            override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): Payload? {
                payload.setDragActor(createDragActor(window.titleLabel.text.toString(), dragColor))
                payload.`object` = window
                payload.validDragActor?.color = dragColor
                return payload
            }

            override fun dragStop(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int,
                payload: Payload?,
                target: Target?
            ) {
                window.color = Color.WHITE
                window.color.a = 1f
            }
        }

        // Create target (dropping onto the window)
        val target = object : Target(window) {
            override fun drag(source: Source, payload: Payload, x: Float, y: Float, pointer: Int): Boolean {
                val sourceWindow = payload.`object` as? VisWindow ?: return false
                val allowDrop = sourceWindow != this.actor && hitTest(x, y, sourceWindow)

                if (allowDrop) {
                    window.background = highlightDrawable
                } else {
                    window.color = Color.WHITE
                }

                return allowDrop
            }

            override fun reset(source: Source?, payload: Payload?) {
                window.background = previousBackground
            }

            override fun drop(source: Source, payload: Payload, x: Float, y: Float, pointer: Int) {
                val sourceWindow = payload.`object` as VisWindow
                val targetWindow = this.actor as VisWindow

                swapWindowContent(sourceWindow, targetWindow)
            }
        }

        // Add source and target to drag and drop
        dragAndDrop.addSource(source)
        dragAndDrop.addTarget(target)

        // Store source and target for later removal
        dragSources[window] = source
        dragTargets[window] = target
    }

    /**
     * Tests if a point hits a window.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @param window The window to test
     * @return True if the point hits the window, false otherwise
     */
    private fun hitTest(x: Float, y: Float, window: VisWindow): Boolean {
        val hit = window.hit(x, y, true)
        return hit != null
    }

    /**
     * Creates a drag actor for the drag and drop system.
     * 
     * @param text The text to display
     * @param color The color of the actor
     * @return The created actor
     */
    private fun createDragActor(text: String, color: Color): Actor {
        val style = VisUI.getSkin().get(LabelStyle::class.java)
        val label = VisLabel(text, style)
        label.color = color

        val bgPixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        bgPixmap.setColor(Color(0.3f, 0.3f, 0.3f, color.a))
        bgPixmap.fill()
        val bgTexture = Texture(bgPixmap)
        bgPixmap.dispose()

        label.style = LabelStyle(label.style.font, color)
        label.touchable = Touchable.disabled

        return label
    }

    /**
     * Swaps the content between two windows.
     * 
     * @param windowA The first window
     * @param windowB The second window
     */
    private fun swapWindowContent(windowA: VisWindow, windowB: VisWindow) {
        if (windowA == windowB) return

        Gdx.app.log("DnD", "Swapping content between '${windowA.titleLabel.text}' and '${windowB.titleLabel.text}'")

        val contentA = windowA.userObject as? Actor
        val contentB = windowB.userObject as? Actor

        if (contentA == null || contentB == null) {
            Gdx.app.error("DnD", "Cannot swap content, userObject not set correctly on one or both windows.")
            return
        }

        val cellA = windowA.getCell(contentA)
        val cellB = windowB.getCell(contentB)

        if (cellA == null || cellB == null) {
            Gdx.app.error("DnD", "Could not find cells for content Actors. Swap failed.")
            return
        }

        // Swap the actors in the cells
        cellA.setActor(contentB)
        cellB.setActor(contentA)

        // Swap the userObjects
        windowA.userObject = contentB
        windowB.userObject = contentA

        // Swap the titles
        val titleA = windowA.titleLabel.text.toString()
        val titleB = windowB.titleLabel.text.toString()
        windowA.titleLabel.setText(titleB)
        windowB.titleLabel.setText(titleA)

        // Invalidate layouts
        windowA.invalidateHierarchy()
        windowB.invalidateHierarchy()
        invalidateHierarchy()
    }

    /**
     * Creates a dockable window with the given title and content.
     * 
     * @param title The title of the window
     * @param addLotsOfContent Whether to add lots of content (for testing)
     * @return The created window
     */
    fun createDockableWindow(title: String, addLotsOfContent: Boolean = false): VisWindow {
        val window = VisWindow(title)
        window.isMovable = false
        window.isResizable = false
        window.titleLabel.setAlignment(Align.left)

        val contentTable = VisTable()
        val actualContent: Actor

        if (addLotsOfContent) {
            for (i in 1..30) {
                contentTable.add(VisLabel("Item $i in $title")).left().pad(2f).row()
            }
            val scrollPane = VisScrollPane(contentTable)
            scrollPane.setFadeScrollBars(false)
            actualContent = scrollPane
        } else {
            contentTable.add(VisLabel("$title Content Area")).pad(10f).row()
            contentTable.add(VisLabel("Some data here.")).pad(5f).row()
            contentTable.add(VisLabel("More details...")).pad(5f).row()
            actualContent = contentTable
        }

        window.add(actualContent).grow().pad(5f)
        window.userObject = actualContent

        return window
    }
}
