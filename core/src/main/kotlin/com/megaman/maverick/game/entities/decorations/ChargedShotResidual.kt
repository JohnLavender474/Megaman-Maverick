package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class ChargedShotResidual(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity,
    IFaceable, IDirectional {

    companion object {
        const val TAG = "ChargedShotResidual"
        private const val DUR = 0.2f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var direction: Direction
    override lateinit var facing: Facing

    var fullyCharged = false
        private set

    private lateinit var position: Position
    private val spawn = Vector2()
    private val timer = Timer(DUR)

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.DECORATIONS_1.source)
            regions.put("full", atlas.findRegion("FullChargedShotResidual"))
            regions.put("half", atlas.findRegion("ChargedShot_Residual_Half"))
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!
        direction = spawnProps.get(ConstKeys.DIRECTION, Direction::class)!!
        fullyCharged = spawnProps.get(ConstKeys.BOOLEAN, Boolean::class)!!
        position = spawnProps.get(ConstKeys.POSITION, Position::class)!!

        spawn.set(spawnProps.get(ConstKeys.SPAWN, Vector2::class)!!)

        timer.reset()

        val dimensions = if (fullyCharged) 1.5f * ConstVals.PPM else ConstVals.PPM.toFloat()
        defaultSprite.setSize(dimensions)
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
        component.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
            sprite.setFlip(
                when {
                    direction.equalsAny(Direction.UP, Direction.LEFT) -> isFacing(Facing.LEFT)
                    else -> isFacing(Facing.RIGHT)
                },
                false
            )
            sprite.setPosition(spawn, position)
            when {
                direction.isVertical() -> sprite.translateX(0.25f * ConstVals.PPM * facing.value)
                else -> sprite.translateY(0.25f * ConstVals.PPM * facing.value)
            }
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (fullyCharged) "full" else "half" }
        val animations = objectMapOf<String, IAnimation>(
            "full" pairTo Animation(regions["full"], 2, 2, 0.05f, false),
            "half" pairTo Animation(regions["half"], 2, 1, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.DECORATION
}
