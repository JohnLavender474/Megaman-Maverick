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
                    getDrawOffset(entity)
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

    // TODO: an entity with no body gets offset 0, and a body that another entity moves outside the world steps gets
    //  only its own step move, so a sprite on another entity's body can be drawn up to 1 step away from it (item
    //  134, branch 134b)
    private fun getDrawOffset(entity: IGameEntity): Vector2 {
        val offsets = drawOffsets ?: return drawOffset.setZero()
        val body = entity.getComponent(BodyComponent::class)?.body ?: return drawOffset.setZero()
        return offsets.getDrawOffset(body, drawOffset)
    }
}
