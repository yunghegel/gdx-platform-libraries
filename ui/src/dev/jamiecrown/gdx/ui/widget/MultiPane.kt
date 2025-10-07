package dev.jamiecrown.gdx.ui.widget

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.building.OneColumnTableBuilder
import com.kotcrab.vis.ui.building.utilities.Padding
import com.ray3k.stripe.PopTable
import dev.jamiecrown.gdx.ui.UI
import dev.jamiecrown.gdx.ui.scene2d.SLabel
import dev.jamiecrown.gdx.ui.scene2d.SMultiSplitPane
import dev.jamiecrown.gdx.ui.scene2d.STable

import kotlin.apply
import kotlin.collections.forEach
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.min

interface Range {

    var start : Float

    var end : Float

    var prefererred : Float

    val length : Float
        get() = end - start



    fun toNormalized(total : Float) : Range {
        val start = this.start / total
        val end = this.end / total
        return Range.of(start, end, prefererred / total)
    }

    val format : String
        get() = "[$start -> $end ($length)]"


    companion object {
        fun of(start : Float, end : Float, pref: Float=0.5f) : Range {
            return object : Range {
                override var start = start
                override var end = end
                override var prefererred = pref
            }
        }
        fun of (start: ()-> Float, end: ()-> Float, pref: ()-> Float) : Range {
            return object : Range {
                override var start :Float = start()
                    get() { return start() }
                override var end :Float = end()
                    get() { return end() }
                override var prefererred :Float = pref()
                    get() { return pref() }
            }
        }
    }


}

class MultiPane(val numWidgets : Int,vertical: Boolean = false)  : SMultiSplitPane(vertical) {
    enum class Adjustment {
        START,MIDDLE, END
    }

    val adj  = { index: Int ->
        when (index) {
            0 -> Adjustment.START
            1 -> Adjustment.END
            else -> Adjustment.MIDDLE
        }
    }



    val ranges = mutableMapOf<Actor,Range>()

    val cache = mutableMapOf<Actor, Float>()

    val prefs = mutableMapOf<Actor, Float>()
    val hidden : MutableMap<Actor, Boolean>
        get() = mutableMapOf<Actor,Boolean>().apply {
            children.forEachIndexed { i,actor ->
                when(i) {
                    0 -> {
                        this[actor] = actor.isVisible
                    }
                    1 -> {
                        this[actor] = actor.isVisible
                    }
                    else -> {
                        this[actor] = actor.isVisible
                    }
                }
            }

        }
    val rangeResolution = mutableMapOf<Actor, (Actor)->Range>()

    val constraintCalculations = mutableMapOf<Actor, (Actor)->Float>()

    override fun setWidgets(vararg actors: Actor) {
        val stacks = actors.map { actor -> Stack(actor) }.toTypedArray()
        stacks.forEachIndexed { index, actor ->
            val popTable : PopTable = PopTable(UI.skin)
            val collapseActor : CollapseActor = CollapseActor(index, actor, actor.children.first(), this)

        }
        super.setWidgets(*stacks)
        val totalActors = actors.size
        stacks.forEachIndexed { index, actor ->
            val start : ()->Float = {
                when (index) {
                    0 ->{  0f }
                    else -> { index.toFloat() / totalActors }
                }
            }
            val end : ()->Float = {
                when (index) {
                    totalActors - 1 -> { 1f }
                    else -> { (index + 1).toFloat() / totalActors }
                }
            }
            ranges[actor.children.first()] = Range.of(start, end, { 0.5f})
            cache[actor.children.first()] = 0f
            prefs[actor.children.first()] = if (actor.children.first() is Layout)( actor.children.first() as Layout).prefWidth/width else 0f

            setRangeResolition(index, { actor ->
                val range = ranges[actor] ?: return@setRangeResolition Range.of(0f, 1f)
                val start = range.start
                val end = range.end
                val pref = range.prefererred
                Range.of(start, end, pref)
            })

            actor.add(
                createDebugActor(actor.children.first(  )))
        }
    }

    fun createDebugActor(actor: Actor) : STable {
        return STable().apply {
            val range = ranges[actor] ?: Range.of(0f, 1f)
            val table = OneColumnTableBuilder(4,4)
            table.append(SLabel("Start: ${range.start}") { range.start.toString() })
            table.append(SLabel("End: ${range.end}") { range.end.toString() })
            table.append(SLabel("Length: ${cache[actor]}") { cache[actor].toString() })
            table.append(SLabel("Preferred: ${range.prefererred}") { range.prefererred.toString() })
            table.setTablePadding(Padding(5f))

            add(table.build()).growX().top()

        }
    }

    fun rangeResolutionFor(actor: Actor, resolution: (Actor)->Range) {
        rangeResolution[actor] = resolution
    }


    override fun setSplit(handleBarIndex: Int, split: Float) {
        val adjustment = adj(handleBarIndex)
        val actor = when (adjustment) {
            Adjustment.START -> children.first()
            Adjustment.END -> children.last()
            Adjustment.MIDDLE -> children[handleBarIndex]
        }
        val proposed = split
        val constrained = constrainedOrValid(actor,proposed,adjustment)
        if (!constrained.isNaN() or constrained.isInfinite()) {
            cache[actor] = constrained
            println("Constrained: $constrained")
            super.setSplit(handleBarIndex, constrained)
            return
        } else {
            super.setSplit(handleBarIndex, cache[actor] ?: prefs[actor] ?: 0f)
        }
    }

    fun setSplitInternal(handleBarIndex: Int, split: Float) {
        super.setSplit(handleBarIndex, split)
    }

    fun setRangeResolition(index: Int, resolution: (Actor)->Range) {
        val actor = children[index]
        rangeResolution[actor] = resolution
    }

    fun createShowHideActor(stack: Stack) : STable {
        return STable().apply {
            val table = OneColumnTableBuilder(4,4)
            table.append(SLabel("Show/Hide: ${stack.children.first()}") { stack.children.first().toString() })
            table.append(SLabel("Split: ${cache[stack]}") { cache[stack].toString() })
            table.setTablePadding(Padding(5f))

            add(table.build()).growX().top()

        }
    }

    fun constrainedOrValid(actor: Actor, proposed: Float,adjustment: Adjustment) : Float {
        val constraint = prefs[actor] ?: -1f
        if (constraint == -1f) return proposed
        val range = ranges[actor] ?: return proposed
        val min = range.start
        val max = range.end

        return when(adjustment) {
            Adjustment.START -> if (!vertical) max(proposed,constraint) else min(proposed,constraint)
            else -> if (!vertical) min(proposed,constraint) else max(proposed,constraint)
        }
    }

    override fun layout() {
        super.layout()
        children.forEach { actor ->
            rangeResolution[actor]?.let { resolution ->
                val range  = resolution(actor)
                ranges[actor] = range
            }
        }
    }

    fun restore(align: Int,actor:Actor) {
        actor.isVisible = true

        when(align) {
            Align.left -> {
                val split = cache[actor] ?: prefs[actor] ?: 0f
                setSplit(0, split)
            }
            Align.right -> {
                val cached = cache[actor] ?: prefs[actor] ?: 0f
                setSplit(1, cached)
            }
            Align.bottom -> {
                val cached = cache[actor] ?: prefs[actor] ?: 0.8f
                setSplit(0, cached)
            }
        }

    }

    fun hide(actor: Actor) {
        actor.isVisible = false
        val index = children.indexOf(actor)
        if (index == -1) return
        when (index) {
            0 -> setSplit(0, 0f)
            1 -> setSplit(1, 0f)
            else -> setSplit(index, 0f)
        }

    }


}