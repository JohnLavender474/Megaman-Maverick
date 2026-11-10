package com.maverick.game.engine.drawables.sprites

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.diagnostics.RuntimeDiagnostics
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.world.IDrawOffsets
import com.maverick.game.engine.world.body.BodyComponent

class SpritesSystem(
    private val collector: (GameSprite) -> Unit,
    private val diagnostics: RuntimeDiagnostics? = null,
    private val drawOffsets: IDrawOffsets? = null
) :
    GameSystem(SpritesComponent::class) {

    private val drawOffset = Vector2()

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        diagnostics?.beginEntry("SpritesSystem")

        entities.forEach { entity ->
            try {
                val component = entity.getComponent(SpritesComponent::class)!!
                if (component.doUpdateAndDraw.invoke(delta)) {
                    component.preProcess(delta)
                    getDrawOffset(entity, component)
                    component.sprites.values().forEach { sprite ->
                        sprite.drawOffsetX = drawOffset.x
                        sprite.drawOffsetY = drawOffset.y
                        collector.invoke(sprite)
                    }
                    component.postProcess(delta)
                }
            } catch (e: Exception) {
                throw Exception("Exception occured while processing sprites for entity: $entity", e)
            }
        }

        diagnostics?.endEntry()
    }

    // TODO: a body that code moves once per frame outside the world steps (for example D1000) has a step move of 0,
    //  so it is drawn at the frame time, up to 1 step ahead of the camera and the stepped bodies (item 134, the
    //  riders and frame-moved bodies item from the 134b docs/TODO.md)
    private fun getDrawOffset(entity: IGameEntity, component: SpritesComponent): Vector2 {
        val offsets = drawOffsets ?: return drawOffset.setZero()
        val body = component.drawOffsetBody?.invoke()
            ?: entity.getComponent(BodyComponent::class)?.body
            ?: return drawOffset.setZero()
        return offsets.getDrawOffset(body, drawOffset)
    }
}
