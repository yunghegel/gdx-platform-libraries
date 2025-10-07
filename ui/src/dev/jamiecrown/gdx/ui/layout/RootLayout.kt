package dev.jamiecrown.gdx.ui.layout


import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.kotcrab.vis.ui.widget.Menu
import com.kotcrab.vis.ui.widget.MenuBar
import com.kotcrab.vis.ui.widget.MenuItem
import dev.jamiecrown.gdx.ui.scene2d.SMultiSplitPane
import dev.jamiecrown.gdx.ui.scene2d.STable
import ktx.actors.onClick

class RootLayout : STable() {

    private val pane : SMultiSplitPane = SMultiSplitPane()

    private val layout = object {
        val menubar = Container<Table>()
        val menu = MenuBar()
        val menus = mutableMapOf<String, Menu>()
        val left = Stack()
        val center = Stack()
        val right = Stack()
        val bottom = Container<STable>()

        val leftTable = STable()
        val centerTable = STable()
        val rightTable = STable()

        init {
            menubar.actor = menu.table
            left.add(leftTable)
            center.add(centerTable)
            right.add(rightTable)
        }
    }

    init {
        add(layout.menubar).expandX().fillX().height(22f).row()
        add(pane).expand().fill().row()
        setFillParent(true)
        pane.setWidgets(
            layout.left,
            layout.center,
            layout.right
        )
        add(layout.bottom).expandX().fillX().height(22f).row()
        debug=true
    }

    fun addMenu(name: String) {
        val item = Menu(name)
        layout.menu.addMenu(item)
        layout.menus[name] = item
    }

    fun addMenubarItem(menu: String,name: String, icon: String, action: () -> Unit) {
        val item = layout.menus[menu]
        item?.let {
            it.addItem(MenuItem(name).apply {
                onClick { action() }
            })
        }


    }



}