package dev.jamiecrown.gdx.ui.scene2d

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import dev.jamiecrown.gdx.state.inject

open class SContainer<A: Actor>(var initial : A)  : Container<A>() {

    var previous: A? = null

    val dnd : DragAndDrop by inject()

    init {
       swap(initial)
        previous = null


    }

    fun swap(new : A) {
        previous = actor
        setActor(new)

        dnd.addTarget(createSwapTarget(new))
        dnd.addSource(createSwapSource(new))
    }

    fun createSwapTarget(a: A) : DragAndDrop.Target {

        return object : DragAndDrop.Target(a) {
            override fun drag(
                source: DragAndDrop.Source?,
                payload: DragAndDrop.Payload?,
                x: Float,
                y: Float,
                pointer: Int
            ): Boolean {
                println("${source!!.actor::class.java.isAssignableFrom(actor::class.java)}")
                return true
            }

            override fun drop(
                source: DragAndDrop.Source?,
                payload: DragAndDrop.Payload?,
                x: Float,
                y: Float,
                pointer: Int
            ) {
                if (source!!.actor::class.java.isAssignableFrom(actor::class.java)) {
                    swap(source.actor as A)
                    whenSwapped(previous!!, source.actor as A)
                }
            }

        }

    }

    fun createSwapSource(a: A ) : DragAndDrop.Source {
        return object : DragAndDrop.Source(a) {
            override fun dragStart(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int
            ): DragAndDrop.Payload? {
                val payload = DragAndDrop.Payload()
                payload.setDragActor(
                    SLabel(
                        "Dragging")
                )
                return payload

            }

            override fun dragStop(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int,
                payload: DragAndDrop.Payload?,
                target: DragAndDrop.Target?
            ) {
                payload?.let {
                    val actor = SLabel(this.toString())
                    payload.setDragActor(actor)

                }
            }

            override fun drag(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int
            ) {
                println("${event?.target?.name} dragging")
                super.drag(event, x, y, pointer)
            }

            override fun getActor(): Actor? {
                return this@SContainer.actor
            }
        }
    }

    open fun whenSwapped(old: A, new: A) {

    }

}