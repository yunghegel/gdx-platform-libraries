package dev.jamiecrown.gdx.ui.scene2d

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable

class TabPane(private var style: TabPaneStyle) : Table() {

    class TabPaneStyle {
        var panesBackground: Drawable? = null
        var tabsBackground: Drawable? = null
        var tabButtonStyle: TextButtonStyle? = null

        constructor()
        constructor(tabButtonStyle: TextButtonStyle?) : super() {
            this.tabButtonStyle = tabButtonStyle
        }
    }

    private val stack = Stack()
    private val tabs = Table()
    private val group = ButtonGroup<Button?>()
    private var defaultPane: Actor? = null
    var extraTabs: Table? = null

    constructor(skin: Skin, tabButtonStyle: String?) : this(
        TabPaneStyle(
            skin.get<TextButtonStyle>(
                tabButtonStyle,
                TextButtonStyle::class.java
            )
        )
    )

    init {
        val backTable = Table()
        if (style.panesBackground != null) {
            backTable.background = style.panesBackground
        }

        val tabsBack = Table()
        if (style.tabsBackground != null) {
            tabsBack.background = style.tabsBackground
        }


        add(tabsBack).fillX()
        tabsBack.add(tabs).bottom()
        tabsBack.add(Table(skin).also { extraTabs = it }).expandX().right().padBottom(4f)
        row()
        add(backTable).grow()
        backTable.add(stack).expand().top()
    }

    fun addPane(name: String?, content: Actor?): Button {
        val bt = TextButton(name, style.tabButtonStyle)
        addPane(bt, content)
        return bt
    }

    fun addPane(tab: Button, content: Actor?) {
        group.setMinCheckCount(0)
        tabs.add(tab)

        val t = Table()
        t.add(content).expandY().top().growX()

        stack.add(t)
        t.isVisible = false
        group.add(tab)
        group.setMinCheckCount(1)
        tab.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                if (tab.isChecked) showPane(group.checkedIndex)
            }
        })
    }

    fun defaultPane(actor: Actor?) {
        defaultPane = actor
        if (group.checked == null) {
            stack.add(actor)
        }
    }

    private fun showPane(index: Int) {
        if (defaultPane != null) defaultPane!!.remove()
        for (c in stack.children) {
            c.isVisible = index < stack.children.size && c === stack.getChild(index)
        }
    }

    var currentIndex: Int
        get() = group.checkedIndex
        set(index) {
            group.buttons[index]!!.isChecked = true
            showPane(index) // force show pane if already the current index.
        }
}
