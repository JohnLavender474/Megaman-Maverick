package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.AnimationUtils

class DrWily(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity, Updatable, IFaceable {

    companion object {
        const val TAG = "DrWily"
        private const val SPRITE_SIZE = 2f
        private const val FLY_UP_SPEED = 12f
        private const val FALL_SPEED = 12f
        private const val BEG_DUR = 1f
        private const val STILL_DUR = 1f
        private val animDefs = orderedMapOf(
            "jump" pairTo AnimationDef(),
            "still" pairTo AnimationDef(),
            "beg" pairTo AnimationDef(2, 1, 0.1f, true),
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class DrWilyState { FLY_UP, FALL, LAND }

    override lateinit var facing: Facing

    private lateinit var state: DrWilyState

    private val position = Vector2()
    private var target = Vector2()
    private var maxY = 0f

    private val begTimer = Timer(BEG_DUR)
    private val stillTimer = Timer(STILL_DUR)

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.WILY_FINAL_BOSS.source)
            animDefs.keys().forEach {
                val region = atlas.findRegion("defeated/$it")
                regions.put(it, region)
            }
        }
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(UpdatablesComponent({ delta -> update(delta) }))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        this.position.set(position)

        val target = spawnProps.get(ConstKeys.TARGET, Vector2::class)!!
        this.target.set(target)

        val maxY = spawnProps.get(ConstKeys.MAX_Y, Float::class)!!
        this.maxY = maxY

        state = DrWilyState.FLY_UP

        begTimer.reset()
        stillTimer.setToEnd()
    }

    override fun update(delta: Float) {
        when (state) {
            DrWilyState.FLY_UP -> {
                position.y += FLY_UP_SPEED * ConstVals.PPM * delta
                if (position.y >= maxY) {
                    position.x = target.x
                    state = DrWilyState.FALL
                    facing = if (megaman.body.getX() > position.x) Facing.RIGHT else Facing.LEFT
                }
            }

            DrWilyState.FALL -> {
                position.y -= FALL_SPEED * ConstVals.PPM * delta
                if (position.y <= target.y) {
                    position.set(target)
                    state = DrWilyState.LAND
                }
            }

            DrWilyState.LAND -> {
                begTimer.update(delta)
                if (begTimer.isJustFinished()) stillTimer.reset()
                if (begTimer.isFinished()) {
                    stillTimer.update(delta)
                    if (stillTimer.isJustFinished())
                        game.eventsMan.submitEvent(Event(EventType.VICTORY_EVENT, props(ConstKeys.END pairTo true)))
                }
            }
        }
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { it.setSize(SPRITE_SIZE * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.setPosition(position, Position.BOTTOM_CENTER)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when (state) {
                        DrWilyState.LAND -> if (begTimer.isFinished()) "still" else "beg"
                        else -> "jump"
                    }
                }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    override fun getType() = EntityType.SPECIAL
}
