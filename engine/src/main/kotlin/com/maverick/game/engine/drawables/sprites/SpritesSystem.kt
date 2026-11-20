package com.maverick.game.engine.drawables.sprites

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.objects.ImmutableCollection
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.diagnostics.RuntimeDiagnostics
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.world.IDrawOffsets
import com.maverick.game.engine.world.WorldWrap
import com.maverick.game.engine.world.body.BodyComponent

class SpritesSystem(
    private val collector: (GameSprite) -> Unit,
    private val diagnostics: RuntimeDiagnostics? = null,
    private val drawOffsets: IDrawOffsets? = null,
    private val wrap: WorldWrap? = null
) :
    GameSystem(SpritesComponent::class) {

    private val drawOffset = Vector2()
    private val wrapRegionOut = GameRectangle()
    private var wrapCount = 0
    private var lastWrapDx = 0f
    private var maxWraps = 0

    override fun process(on: Boolean, entities: ImmutableCollection<IGameEntity>, delta: Float) {
        if (!on) return

        diagnostics?.beginEntry("SpritesSystem")

        val wrapRegion = wrap?.region?.invoke(wrapRegionOut)

        entities.forEach { entity ->
            try {
                val component = entity.getComponent(SpritesComponent::class)!!
                if (component.doUpdateAndDraw.invoke(delta)) {
                    component.preProcess(delta)
                    getDrawOffset(entity, component)
                    val entityWrapRegion = getEntityWrapRegion(wrapRegion, entity)
                    component.sprites.values().forEach { sprite ->
                        sprite.drawOffsetX = drawOffset.x
                        sprite.drawOffsetY = drawOffset.y
                        sprite.wrapOffsetX = getWrapOffsetX(sprite, entityWrapRegion)
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

    private fun getEntityWrapRegion(region: GameRectangle?, entity: IGameEntity): GameRectangle? {
        if (region == null || wrap == null || !wrap.wraps(entity)) return null
        val body = entity.getComponent(BodyComponent::class)?.body
        wrapCount = body?.wrapCount ?: 0
        lastWrapDx = body?.lastWrapDx ?: 0f
        maxWraps = wrap.maxWraps(entity)
        return region
    }

    private fun getWrapOffsetX(sprite: GameSprite, region: GameRectangle?): Float {
        if (region == null) return 0f
        val box = sprite.boundingRectangle
        val minX = box.x + sprite.drawOffsetX
        return WorldWrap.seamShift(
            minX = minX,
            maxX = minX + box.width,
            region = region,
            wrapCount = wrapCount,
            lastWrapDx = lastWrapDx,
            maxWraps = maxWraps
        )
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
