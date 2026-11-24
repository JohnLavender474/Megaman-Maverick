package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.roundAngleToStep
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class ChargedShotResidual(services: MegaServices) : MegaGameEntity(services), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "ChargedShotResidual"
        private const val DUR = 0.2f
        private const val DRAW_ANGLE_STEP = 45f
        private const val FULL_DIAGONAL = "full_diagonal"
        private const val HALF_DIAGONAL = "half_diagonal"
        private val regions = ObjectMap<String, TextureRegion>()
    }

    var fullyCharged = false
        private set

    private val spawn = Vector2()
    private val timer = Timer(DUR)
    private var rotation = 0f
    private var diagonal = false

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.DECORATIONS_1.source)
            regions.put("full", atlas.findRegion("FullChargedShotResidual"))
            regions.put("half", atlas.findRegion("ChargedShot_Residual_Half"))
            regions.put(FULL_DIAGONAL, atlas.findRegion("FullChargedShotResidual_diagonal"))
            regions.put(HALF_DIAGONAL, atlas.findRegion("ChargedShot_Residual_Half_diagonal"))
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        spawn.set(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)

        fullyCharged = spawnProps.get(ConstKeys.BOOLEAN, Boolean::class)!!

        val dimensions = if (fullyCharged) 1.5f * ConstVals.PPM else ConstVals.PPM.toFloat()
        defaultSprite.setSize(dimensions)

        rotation = spawnProps.get(ConstKeys.ROTATION, Float::class)!!.roundAngleToStep(DRAW_ANGLE_STEP)
        diagonal = rotation % 90f != 0f

        timer.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        timer.update(delta)
        if (timer.isFinished()) destroy()
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = if (diagonal) rotation - DRAW_ANGLE_STEP else rotation
            sprite.setCenter(spawn)
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when {
                fullyCharged && diagonal -> FULL_DIAGONAL
                fullyCharged -> "full"
                diagonal -> HALF_DIAGONAL
                else -> "half"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "full" pairTo Animation(regions["full"], 2, 2, 0.05f, false),
            "half" pairTo Animation(regions["half"], 2, 1, 0.1f, false),
            FULL_DIAGONAL pairTo Animation(regions[FULL_DIAGONAL], 2, 2, 0.05f, false),
            HALF_DIAGONAL pairTo Animation(regions[HALF_DIAGONAL], 2, 1, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.DECORATION
}
