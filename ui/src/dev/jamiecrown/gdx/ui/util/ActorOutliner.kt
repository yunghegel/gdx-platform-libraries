package dev.jamiecrown.gdx.ui.util

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage

class ActorOutliner(val shapeRenderer: ShapeRenderer) {



    fun outline(actor: Actor, color: Color = Color.WHITE, fill: Boolean = false) {
        if (actor.stage != null) setFromStage(actor.stage)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = color
        if (fill) shapeRenderer.rect(actor.x, actor.y, actor.width, actor.height)
        else shapeRenderer.rectLine(actor.x, actor.y, actor.x + actor.width, actor.y, 1f)
        shapeRenderer.end()

    }

    fun setFromStage(stage: Stage) {
        shapeRenderer.projectionMatrix = stage.camera.combined
    }



}