package dev.jamiecrown.gdx.ui.layout

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisTable
import dev.jamiecrown.gdx.ui.scene2d.SMultiSplitPane
import dev.jamiecrown.gdx.ui.scene2d.STextButton

// ... (rest of the imports from the previous version)
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener

import com.kotcrab.vis.ui.widget.VisWindow

class CollapsiblePanelManager(
    private val splitPane: SMultiSplitPane,
    val window: VisWindow, // Store the VisWindow directly now
    private val orientation: PanelOrientation,
    private val collapsedSize: Float = 30f,
    private val splitterIndex: Int,
    private val isFirstElement: Boolean
) {
    private var isCollapsed = false
    private var storedSplitAmount = if (isFirstElement) 0.25f else 0.75f

    val container = VisTable() // This is added to the MultiSplitPane

    // Create a wrapper table that will contain the button and update its position
    private val buttonWrapper = object : VisTable() {
        init {
            // Don't use the default table background
            background = null

            // Set the table to be the same size as the button
            setSize(24f, 24f)
        }

        override fun act(delta: Float) {
            super.act(delta)
            // Update position in each frame to follow the window
            if (stage != null) {
                updateButtonPosition()
            }
        }
    }

    private val collapseButton = STextButton("<").apply {
        // Initial text based on orientation
        setText(if (orientation == PanelOrientation.LEFT || orientation == PanelOrientation.TOP) "<" else ">")
        // Make the button small and discrete
        setSize(24f, 24f)
    }

    // Button offset from the edge of the window
    private val BUTTON_OFFSET = 5f

    // Reference to the actual content Actor inside the window
    private val contentActor: Actor? = window.userObject as? Actor // Get content from userObject

    // Reference to the stage (will be set when the window is added to the stage)
    private var stage: Stage? = null

    init {
        if (contentActor == null) {
            Gdx.app.error("CollapsiblePanelManager", "VisWindow provided must have its main content Actor set as userObject.")
        }
        setupUI()
        setupButtonListener()
        showExpanded() // Start expanded

        // Check if the window already has a stage
        if (window.stage != null) {
            stage = window.stage
            addButtonToStage()
        } else {
            // Add a listener to detect when the window is added to the stage
            window.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    if (window.stage != null && stage == null) {
                        stage = window.stage
                        addButtonToStage()
                    }
                }
            })
        }
    }

    private fun setupUI() {
        // We don't need separate wrappers anymore if the button is part of the layout logic
        // The 'container' will hold only the window content
    }

    private fun addButtonToStage() {
        // Add the button to the wrapper
        buttonWrapper.clearChildren()
        buttonWrapper.add(collapseButton).size(24f, 24f).center()
        buttonWrapper.debug = true

        // Add the wrapper to the stage
        stage?.addActor(buttonWrapper)
        updateButtonPosition()
    }

    private fun updateButtonPosition() {
        // Position the wrapper based on the orientation and window position
        if (!window.isVisible) return // Don't update position if window is not visible

        when (orientation) {
            PanelOrientation.LEFT -> {
                buttonWrapper.setPosition(
                    window.x + window.width + BUTTON_OFFSET,
                    window.y + (window.height - collapseButton.height) / 2
                )
            }
            PanelOrientation.RIGHT -> {
                buttonWrapper.setPosition(
                    window.x - collapseButton.width - BUTTON_OFFSET,
                    window.y + (window.height - collapseButton.height) / 2
                )
            }
            PanelOrientation.TOP -> {
                buttonWrapper.setPosition(
                    window.x + (window.width - collapseButton.width) / 2,
                    window.y - collapseButton.height - BUTTON_OFFSET
                )
            }
            PanelOrientation.BOTTOM -> {
                buttonWrapper.setPosition(
                    window.x + (window.width - collapseButton.width) / 2,
                    window.y + window.height + BUTTON_OFFSET
                )
            }
        }

        // Ensure the button is visible
        buttonWrapper.isVisible = true
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

    private fun collapse() {
        if (isCollapsed) return
        storedSplitAmount = splitPane.splits[splitterIndex]

        val totalSize = if (splitPane.vertical) splitPane.height else splitPane.width
        val collapsedRatio = if (totalSize > 0) collapsedSize / totalSize else 0.05f

        val targetSplit = if (isFirstElement) collapsedRatio else 1.0f - collapsedRatio
        splitPane.setSplit(splitterIndex, targetSplit.coerceIn(0.01f, 0.99f))

        showCollapsed() // Update the layout within 'container'
        isCollapsed = true
        // Ensure button text reflects the "action to expand"
        collapseButton.setText(if (orientation == PanelOrientation.LEFT || orientation == PanelOrientation.TOP) ">" else "<")

        // Update button position after collapse
        updateButtonPosition()
    }

    private fun expand() {
        if (!isCollapsed) return
        splitPane.setSplit(splitterIndex, storedSplitAmount.coerceIn(0.01f, 0.99f))

        showExpanded() // Update the layout within 'container'
        isCollapsed = false
        // Ensure button text reflects the "action to collapse"
        collapseButton.setText(if (orientation == PanelOrientation.LEFT || orientation == PanelOrientation.TOP) "<" else ">")

        // Update button position after expand
        updateButtonPosition()
    }

    // This method now builds the layout directly into 'container'
    private fun showExpanded() {
        container.clearChildren()
        // Only add the window to the container, the button is added to the stage
        container.add(window).grow()
        container.invalidateHierarchy()

        // Make sure the window is visible
        window.isVisible = true
    }

    // This method now builds the layout directly into 'container'
    private fun showCollapsed() {
        container.clearChildren()
        // In collapsed state, the container is empty or can have a placeholder
        // The button is added to the stage and will be the only visible element
        container.invalidateHierarchy()

        // Hide the window when collapsed
        window.isVisible = false
    }

    // Function to update the window reference if content is swapped externally
    // This might not be strictly necessary if only the *content* inside the window changes,
    // but good practice if the window instance itself could potentially change.
    // fun updateManagedWindow(newWindow: VisWindow) {
    //     this.window = newWindow
    //     this.contentActor = window.userObject as? Actor
    //     if(isCollapsed) showCollapsed() else showExpanded() // Rebuild layout
    // }
}
