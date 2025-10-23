package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.extensions.getCenter

class AirConditioner(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity, ISpritesEntity,
    IAnimatedEntity {

    companion object {
        const val TAG = "AirConditioner"
        private const val MACHINE_SPRITE_WIDTH = 4
        private const val MACHINE_SPRITE_HEIGHT = 2
        private const val PIPE_SPRITE_WIDTH = 4
        private const val PIPE_SPRITE_HEIGHT = 1
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val bounds = GameRectangle()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.DECORATIONS_1.source)
            gdxArrayOf("machine", "pipe").forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        val centerX = bounds.getCenter().x
        val startY = bounds.getY()
        val rows = bounds.getHeight().div(ConstVals.PPM.toFloat()).toInt()
        defineDrawables(centerX, startY, rows)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        sprites.clear()
        animators.clear()
        clearSpritePreProcess()
    }

    private fun defineDrawables(centerX: Float, startY: Float, rows: Int) {
        GameLogger.debug(TAG, "defineDrawables(): centerX=$centerX, startY=$startY, rows=$rows")

        val machine = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
        machine.setSize(MACHINE_SPRITE_WIDTH.toFloat() * ConstVals.PPM, MACHINE_SPRITE_HEIGHT.toFloat() * ConstVals.PPM)

        machine.setCenterX(centerX)
        machine.y = startY

        sprites.put("machine", machine)

        val machineAnim = Animation(regions["machine"], 2, 1, 0.1f, true)
        val machineAnimator = Animator(machineAnim)
        putAnimator(machine, machineAnimator)

        GameLogger.debug(TAG, "defineDrawables(): put machine: y=$startY")

        (MACHINE_SPRITE_HEIGHT until rows step PIPE_SPRITE_HEIGHT).forEach { y ->
            val key = "pipe_$y"

            val pipe = GameSprite(regions["pipe"], DrawingPriority(DrawingSection.PLAYGROUND, -1))
            pipe.setSize(PIPE_SPRITE_WIDTH.toFloat() * ConstVals.PPM, PIPE_SPRITE_HEIGHT.toFloat() * ConstVals.PPM)

            pipe.setCenterX(centerX)
            val pipeY = startY + y * ConstVals.PPM
            pipe.y = pipeY

            sprites.put(key, pipe)

            GameLogger.debug(TAG, "defineDrawables(): put pipe: key=$key, y=$pipeY")
        }
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(game.getGameCamera(), { bounds }))
    )

    override fun getType() = EntityType.DECORATION
}
