package dev.jamiecrown.gdx.ui.widget

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import dev.jamiecrown.gdx.ui.UI
import dev.jamiecrown.gdx.ui.layout.DockablePanelLayout
import dev.jamiecrown.gdx.ui.layout.PanelOrientation
import dev.jamiecrown.gdx.ui.scene2d.STextButton

/**
 * An actor that encapsulates a dockable window and its collapse button.
 * This class handles the layout and positioning of the button relative to the window.
 */
class DockableWindowActor(
    val window: VisWindow,
    private val orientation: PanelOrientation,
    private val collapsedSize: Float = 0f,
    private var parentLayout: DockablePanelLayout? = null,
    private var indexInLayout: Int = -1
) : Stack() {

    private var isCollapsed = false
    private var storedSize = 0.25f // Default stored size when expanded

    // The container that holds the window
    private val container = VisTable()

    // Button offset from the edge of the window
    private val BUTTON_OFFSET = 5f

    // The collapse button
    private val collapseButton = STextButton("<").apply {
        // Initial text based on orientation
        setText(if (orientation == PanelOrientation.LEFT || orientation == PanelOrientation.TOP) "<" else ">")
        // Make the button small and discrete
        setSize(24f, 24f)
    }

    // Button wrapper to position the button
    private val buttonWrapper = object : VisTable() {
        init {
            // Don't use the default table background
            background = null

            // Set the table to be the same size as the button
            setSize(24f, 24f)

            // Add the button to the wrapper
            add(collapseButton).size(24f, 24f).center()
        }

        override fun act(delta: Float) {
            super.act(delta)
            // Update position in each frame to follow the window
            if (stage != null) {
                updateButtonPosition()
            }
        }
    }

    init {
        // Add the container to the stack
        add(container)

        debug = true

        // Setup the button listener
        setupButtonListener()

        // Start expanded
        showExpanded()
        UI.stage.addActor(buttonWrapper)
        updateButtonPosition()
        // Add a listener to detect when this actor is added to the stage
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (stage != null) {

                }
            }
        })
    }

    private fun setupButtonListener() {
        collapseButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                toggleCollapse()
            }
        })
    }

    fun toggleCollapse() {
        if (isCollapsed) {
            expand()
        } else {
            collapse()
        }
    }

    fun collapse() {
        if (isCollapsed) return

        // Store the current size before collapsing
        if (parentLayout != null && indexInLayout >= 0) {
            // Get the current split ratio
            val splitIndex = if (indexInLayout == 0) 0 else indexInLayout - 1
            if (splitIndex >= 0 && splitIndex < parentLayout!!.splits.size) {
                storedSize = parentLayout!!.splits[splitIndex]

                // Calculate the collapsed size as a ratio of the total size
                val totalSize = if (parentLayout!!.vertical) parentLayout!!.height else parentLayout!!.width
                val collapsedRatio = if (totalSize > 0) collapsedSize / totalSize else 0f

                // Adjust the split ratio based on the orientation
                val targetSplit = when (orientation) {
                    PanelOrientation.LEFT, PanelOrientation.TOP -> collapsedRatio
                    PanelOrientation.RIGHT, PanelOrientation.BOTTOM -> 1.0f - collapsedRatio
                }

                // Set the split ratio
                parentLayout!!.setSplit(splitIndex, targetSplit)
            }
        }

        showCollapsed()
        isCollapsed = true

        // Ensure button text reflects the "action to expand"
        collapseButton.setText(if (orientation == PanelOrientation.LEFT || orientation == PanelOrientation.TOP) ">" else "<")

        // Update button position after collapse
        updateButtonPosition()
    }

    fun expand() {
        if (!isCollapsed) return

        // Restore the stored size
        if (parentLayout != null && indexInLayout >= 0) {
            val splitIndex = if (indexInLayout == 0) 0 else indexInLayout - 1
            if (splitIndex >= 0 && splitIndex < parentLayout!!.splits.size) {
                parentLayout!!.setSplit(splitIndex, storedSize.coerceIn(0.01f, 0.99f))
            }
        }

        showExpanded()
        isCollapsed = false

        // Ensure button text reflects the "action to collapse"
        collapseButton.setText(if (orientation == PanelOrientation.LEFT || orientation == PanelOrientation.TOP) "<" else ">")

        // Update button position after expand
        updateButtonPosition()
    }

    private fun showExpanded() {
        container.clearChildren()
        // Add the window to the container
        container.add(window).grow()
        container.invalidateHierarchy()

        // Make sure the window is visible
        window.isVisible = true
    }

    private fun showCollapsed() {
        container.clearChildren()
        // In collapsed state, the container is empty
        container.invalidateHierarchy()

        // Hide the window when collapsed
        window.isVisible = false
    }

    private fun updateButtonPosition() {
        // Position the wrapper based on the orientation and window position

        val windowX = if (window.isVisible) window.x else x
        val windowY = if (window.isVisible) window.y else y
        val windowWidth = if (window.isVisible) window.width else width
        val windowHeight = if (window.isVisible) window.height else height

        when (orientation) {
            PanelOrientation.LEFT -> {
                buttonWrapper.setPosition(
                    windowX + windowWidth + BUTTON_OFFSET,
                    windowY + (windowHeight - collapseButton.height) / 2
                )
            }
            PanelOrientation.RIGHT -> {
                buttonWrapper.setPosition(
                    windowX - collapseButton.width - BUTTON_OFFSET,
                    windowY + (windowHeight - collapseButton.height) / 2
                )
            }
            PanelOrientation.TOP -> {
                buttonWrapper.setPosition(
                    windowX + (windowWidth - collapseButton.width) / 2,
                    windowY - collapseButton.height - BUTTON_OFFSET
                )
            }
            PanelOrientation.BOTTOM -> {
                buttonWrapper.setPosition(
                    windowX + (windowWidth - collapseButton.width) / 2,
                    windowY + windowHeight + BUTTON_OFFSET
                )
            }
        }

        // Ensure the button is visible
        buttonWrapper.isVisible = true
    }

    override fun remove(): Boolean {
        // Remove the button wrapper from the stage when this actor is removed
        buttonWrapper.remove()
        return super.remove()
    }

    // Getters and setters for external use
    fun isCollapsed(): Boolean = isCollapsed
    fun getStoredSize(): Float = storedSize
    fun setStoredSize(size: Float) {
        storedSize = size
    }

    // Set the parent layout and index
    fun setParentLayout(layout: DockablePanelLayout, index: Int) {
        parentLayout = layout
        indexInLayout = index
    }
}
