package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IActivatable
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.items.Coin
import com.megaman.maverick.game.entities.special.GrowingVine
import com.megaman.maverick.game.entities.special.GrowingVine.GrowingVineState
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getEntity

class QuestionBlock(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IAnimatedEntity, IActivatable,
    IDirectional, IEventListener {

    companion object {
        const val TAG = "QuestionBlock"

        private const val COIN_CULL_TIME = 0.25f
        private const val COIN_GRAVITY = 0.15f
        private const val COIN_IMPULSE_Y = 5f

        private val animDefs = orderedMapOf(
            "active" pairTo AnimationDef(2, 2, 0.1f, true),
            "inactive" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class QuestionBlockItemType { COIN, GROWING_VINE }

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_DONE_DYIN)
    override lateinit var direction: Direction
    override var on = true

    lateinit var itemType: QuestionBlockItemType
        private set

    private var spawnedItem: MegaGameEntity? = null

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SMB3_BLOCKS.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())

        on = true

        itemType = QuestionBlockItemType.valueOf(
            spawnProps.getOrDefault(
                "${ConstKeys.ITEM}_${ConstKeys.TYPE}",
                QuestionBlockItemType.COIN.name,
                String::class
            ).uppercase()
        )
        if (itemType == QuestionBlockItemType.GROWING_VINE) putProperty(
            "${ConstKeys.GROW}_${ConstKeys.BOUNDS}",
            spawnProps.get("${ConstKeys.GROW}_${ConstKeys.BOUNDS}", RectangleMapObject::class)!!.rectangle
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        spawnedItem = null
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")
        if (event.key == EventType.PLAYER_DONE_DYIN) {
            if (spawnedItem?.spawned == true) spawnedItem!!.destroy()
            spawnedItem = null
            on = true
        }
    }

    override fun hitByHead(processState: ProcessState, headFixture: IFixture) {
        if (processState == ProcessState.END || !on) {
            GameLogger.debug(TAG, "hitByHead(): do not process, return early")
            return
        }

        GameLogger.debug(TAG, "hitByHead(): processState=$processState, headFixture=$headFixture")

        val entity = headFixture.getEntity()
        if (entity != megaman) return

        val shouldBeHit = when (direction) {
            Direction.UP -> megaman.body.physics.velocity.y >= 0f
            Direction.DOWN -> megaman.body.physics.velocity.y <= 0f
            Direction.LEFT -> megaman.body.physics.velocity.x <= 0f
            Direction.RIGHT -> megaman.body.physics.velocity.x >= 0f
        }
        if (shouldBeHit) {
            spawnedItem = spawnItem()
            on = false
        }
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { it.setSize(ConstVals.PPM.toFloat()) })
        .preProcess { _, sprite -> sprite.setCenter(body.getCenter()) }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (on) "active" else "inactive" }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    private fun spawnItem(): MegaGameEntity {
        when (itemType) {
            QuestionBlockItemType.COIN -> {
                val position = body.getBounds().getPositionPoint(DirectionPositionMapper.getPosition(direction))

                val impulse = GameObjectPools.fetch(Vector2::class)
                    .set(0f, COIN_IMPULSE_Y * ConstVals.PPM)

                val coin = MegaEntityFactory.fetch(Coin::class)!!
                coin.spawn(
                    props(
                        ConstKeys.IMPULSE pairTo impulse,
                        ConstKeys.POSITION pairTo position,
                        ConstKeys.GRAVITY pairTo COIN_GRAVITY,
                        ConstKeys.CULL_TIME pairTo COIN_CULL_TIME,
                        "${ConstKeys.PLAYER}_${ConstKeys.CONTACT}" pairTo false
                    )
                )

                val coins = game.getOrDefaultProperty(ConstKeys.COINS, 0, Int::class)
                game.putProperty(ConstKeys.COINS, coins + 1)

                playSoundNow(SoundAsset.SMB3_COIN_SOUND, false)

                return coin
            }
            QuestionBlockItemType.GROWING_VINE -> {
                val growBounds =
                    getProperty("${ConstKeys.GROW}_${ConstKeys.BOUNDS}", Rectangle::class)!!.toGameRectangle()

                val growingVine = MegaEntityFactory.fetch(GrowingVine::class)!!
                growingVine.spawn(
                    props(
                        ConstKeys.UP pairTo true,
                        ConstKeys.BOUNDS pairTo growBounds,
                        ConstKeys.STATE pairTo GrowingVineState.GROWING
                    )
                )

                return growingVine
            }
        }
    }
}
