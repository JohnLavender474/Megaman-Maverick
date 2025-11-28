package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.decorations.FloatingEmber
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPosition
import com.megaman.maverick.game.utils.extensions.getRandomPositionInBounds
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getPosition

class RisingLavaRiver(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IAudioEntity, IEventListener {

    companion object {
        const val TAG = "RisingLavaRiver"

        private const val DEFAULT_RISE_SPEED = 1.5f
        private const val DEFAULT_FALL_SPEED = 10f

        private const val DIST_TRIGGER_SPEED_UP = 15f
        private const val RISE_SPEED_UP_SCALAR = 2.5f

        private const val STOP_DELAY = 0.25f

        private const val SHAKE_X = 0f
        private const val SHAKE_Y = 0.005f

        private const val RISE_SHAKE_DELAY = 2f
        private const val FALL_SHAKE_DELAY = 1f

        private const val RISE_SHAKE_DUR = 1f
        private const val RISE_SHAKE_INTERVAL = 0.1f

        private const val EMBER_MIN_DELAY = 0.5f
        private const val EMBER_MAX_DELAY = 0.75f
        private const val EMBER_SPAWN_X_BUFFER = ConstVals.VIEW_WIDTH + 6
        private const val EMBER_SPAWN_Y_BUFFER = ConstVals.VIEW_HEIGHT + 12
        private const val EMBER_CULL_TIME = 3f

        private val animDefs = orderedMapOf(
            RisingLavaRiverType.TOP pairTo AnimationDef(3, 1, 0.1f, true),
            RisingLavaRiverType.INNER pairTo AnimationDef(3, 1, 0.1f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class RisingLavaRiverType { TOP, INNER }

    private enum class RisingLavaRiverState { DORMANT, RISING, STOPPED, FALLING }

    override val eventKeyMask = objectSetOf<Any>(
        EventType.PLAYER_READY,
        EventType.PLAYER_DONE_DYIN,
        EventType.BEGIN_ROOM_TRANS,
        EventType.GATE_INIT_OPENING,
        EventType.END_ROOM_TRANS,
        EventType.SET_TO_ROOM_NO_TRANS
    )

    private val startPosition = Vector2()

    private val deathBounds = GameRectangle()
    private lateinit var deathFixture: IFixture

    // the lava rises only for a single room; for all other rooms the lava should be lowered and dormant
    private lateinit var riseRoom: String
    private lateinit var state: RisingLavaRiverState

    private val shakeDelay = Timer()
    private val stopDelay = Timer(STOP_DELAY)

    private var riseSpeed = 0f
    private var fallSpeed = 0f

    private var left = false
    private var hidden = true
    private var allowSpeedUp = false

    private val out = Matrix<GameRectangle>()

    private val emberDelay = Timer()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_2.source)
            animDefs.keys().forEach {
                val key = it.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(AnimationsComponent())
        addComponent(SpritesComponent(
            doUpdateAndDraw = {
                state.equalsAny(RisingLavaRiverState.RISING, RisingLavaRiverState.FALLING)
            }
        ))
        addComponent(AudioComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        val canSpawn = super.canSpawn(spawnProps) &&
            (game.getCurrentLevel() != LevelDefinition.INFERNO_MAN ||
                !LevelUtils.isInfernoManLevelFrozen(game.state))

        GameLogger.debug(TAG, "canSpawn(): canSpawn=$canSpawn")

        return canSpawn
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        state = RisingLavaRiverState.DORMANT

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)
        deathBounds.set(bounds)

        startPosition.set(bounds.getPosition())

        riseRoom = spawnProps.get("${ConstKeys.RISE}_${ConstKeys.ROOM}", String::class)!!

        riseSpeed = spawnProps.getOrDefault("${ConstKeys.RISE}_${ConstKeys.SPEED}", DEFAULT_RISE_SPEED, Float::class)
        fallSpeed = spawnProps.getOrDefault("${ConstKeys.FALL}_${ConstKeys.SPEED}", DEFAULT_FALL_SPEED, Float::class)

        left = spawnProps.getOrDefault(ConstKeys.LEFT, false, Boolean::class)
        allowSpeedUp = spawnProps.getOrDefault(
            "${ConstKeys.ALLOW}_${ConstKeys.SPEED}_${ConstKeys.UP}", true, Boolean::class
        )

        val type = RisingLavaRiverType.valueOf(
            spawnProps.get(ConstKeys.TYPE, String::class)!!.uppercase()
        )
        defineDrawables(bounds, type)

        setLavaToDormant()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        game.eventsMan.removeListener(this)

        sprites.clear()
        animators.clear()
        clearSpritePreProcess()
    }

    override fun onEvent(event: Event) {
        val key = event.key

        GameLogger.debug(TAG, "onEvent(): event.key=$key")

        when (key) {
            EventType.PLAYER_READY -> {
                val room = game.getCurrentRoom()!!.name
                if (room == riseRoom) setLavaToRising() else setLavaToDormant()
            }
            EventType.PLAYER_DONE_DYIN -> setLavaToDormant()
            EventType.BEGIN_ROOM_TRANS ->
                if (state == RisingLavaRiverState.RISING) {
                    val room = game.getCurrentRoom()!!.name
                    if (room != riseRoom) setLavaToStopped()
                }
            EventType.GATE_INIT_OPENING -> setLavaToStopped()
            EventType.END_ROOM_TRANS, EventType.SET_TO_ROOM_NO_TRANS -> {
                val room = game.getCurrentRoom()!!.name
                if (room == riseRoom) setLavaToRising() else setLavaToDormant()
            }
        }
    }

    private fun defineDrawables(bounds: GameRectangle, type: RisingLavaRiverType) = bounds
        .splitByCellSize(
            8f * ConstVals.PPM,
            (if (type == RisingLavaRiverType.TOP) 1f else 8f) * ConstVals.PPM, out
        )
        .forEach { column, row, _ ->
            val key = "${column}_${row}"

            val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
            sprite.setSize(
                8f * ConstVals.PPM,
                (if (type == RisingLavaRiverType.TOP) 1f else 8f) * ConstVals.PPM
            )
            sprites.put(key, sprite)
            putSpritePreProcess(key) { _, _ ->
                val bodyPos = body.getPosition()
                val spriteX = bodyPos.x + column * 8f * ConstVals.PPM
                val spriteY = bodyPos.y + row * (if (type == RisingLavaRiverType.TOP) 1f else 8f) * ConstVals.PPM
                sprite.setPosition(spriteX, spriteY)
                sprite.setFlip(left, false)
                sprite.hidden = hidden
            }

            val animDef = animDefs[type]
            val animation = Animation(
                regions[type.name.lowercase()],
                animDef.rows,
                animDef.cols,
                animDef.durations,
                animDef.loop
            )
            val animator = Animator(animation)
            animator.shouldAnimatePredicate = shouldAnimate@{
                return@shouldAnimate state.equalsAny(
                    RisingLavaRiverState.RISING,
                    RisingLavaRiverState.FALLING
                )
            }
            putAnimator(key, sprite, animator)
        }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when (state) {
            RisingLavaRiverState.RISING -> {
                body.physics.velocity.set(0f, riseSpeed * ConstVals.PPM)
                if (shouldIncreaseRiseSpeed()) body.physics.velocity.y *= RISE_SPEED_UP_SCALAR

                emberDelay.update(delta)
                if (emberDelay.isFinished()) {
                    val position = GameObjectPools.fetch(Vector2::class)
                        .setX(body.getBounds().getRandomPositionInBounds().x)
                        .setY(body.getBounds().getMaxY())

                    val canSpawnBounds = GameObjectPools.fetch(GameRectangle::class)
                        .setWidth(EMBER_SPAWN_X_BUFFER * ConstVals.PPM)
                        .setHeight(EMBER_SPAWN_Y_BUFFER * ConstVals.PPM)
                        .setCenter(game.getGameCamera().getRotatedBounds().getCenter())

                    if (canSpawnBounds.contains(position)) {
                        val ember = MegaEntityFactory.fetch(FloatingEmber::class)!!
                        ember.spawn(
                            props(
                                ConstKeys.POSITION pairTo position,
                                ConstKeys.CULL_TIME pairTo EMBER_CULL_TIME
                            )
                        )

                        resetEmberDelay()
                    }
                }

                if (!game.isProperty(ConstKeys.ROOM_TRANSITION, true)) {
                    shakeDelay.update(delta)
                    if (shakeDelay.isFinished()) {
                        game.eventsMan.submitEvent(
                            Event(
                                EventType.SHAKE_CAM, props(
                                    ConstKeys.INTERVAL pairTo RISE_SHAKE_INTERVAL,
                                    ConstKeys.DURATION pairTo RISE_SHAKE_DUR,
                                    ConstKeys.X pairTo SHAKE_X * ConstVals.PPM,
                                    ConstKeys.Y pairTo SHAKE_Y * ConstVals.PPM
                                )
                            )
                        )

                        requestToPlaySound(SoundAsset.QUAKE_SOUND, false)

                        shakeDelay.reset()
                    }
                }
            }
            RisingLavaRiverState.STOPPED -> {
                stopDelay.update(delta)
                if (stopDelay.isFinished()) {
                    GameLogger.debug(TAG, "update(): stop delay finished")
                    setLavaToFalling()
                }
            }
            RisingLavaRiverState.FALLING -> {
                val maxY = body.getMaxY()
                val camY = game.getGameCamera().toGameRectangle().getY()
                if (maxY < camY) {
                    GameLogger.debug(TAG, "update(): falling: maxY=$maxY, camY=$camY")
                    setLavaToDormant()
                    return@UpdatablesComponent
                }
            }
            RisingLavaRiverState.DORMANT -> {
                body.physics.velocity.setZero()
                body.setPosition(startPosition)
                deathFixture.setActive(false)
                hidden = true
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.BLUE

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        deathFixture = Fixture(body, FixtureType.DEATH, deathBounds)
        deathFixture.putProperty(ConstKeys.INSTANT, true)
        body.addFixture(deathFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, doUpdate = {
            state.equalsAny(RisingLavaRiverState.RISING, RisingLavaRiverState.FALLING)
        })
    }

    private fun setLavaToRising() {
        GameLogger.debug(TAG, "setLavaToRising()")
        state = RisingLavaRiverState.RISING
        deathFixture.setActive(true)
        hidden = false
        shakeDelay.resetDuration(RISE_SHAKE_DELAY)
        requestToPlaySound(SoundAsset.QUAKE_SOUND, false)
    }

    private fun setLavaToStopped() {
        GameLogger.debug(TAG, "setLavaToStopped()")
        state = RisingLavaRiverState.STOPPED
        body.physics.velocity.setZero()
        deathFixture.setActive(false)
        hidden = false
        stopDelay.reset()
    }

    private fun setLavaToFalling() {
        GameLogger.debug(TAG, "setLavaToFalling()")
        state = RisingLavaRiverState.FALLING
        body.physics.velocity.set(0f, -fallSpeed * ConstVals.PPM)
        deathFixture.setActive(false)
        hidden = false
        shakeDelay.resetDuration(FALL_SHAKE_DELAY)
    }

    private fun setLavaToDormant() {
        GameLogger.debug(TAG, "setLavaToDormant()")
        state = RisingLavaRiverState.DORMANT
        body.setPosition(startPosition)
        body.physics.velocity.setZero()
        deathFixture.setActive(false)
        animators.values().forEach { it.reset() }
        hidden = true
    }

    private fun resetEmberDelay() {
        val duration = UtilMethods.getRandom(EMBER_MIN_DELAY, EMBER_MAX_DELAY)
        emberDelay.resetDuration(duration)
    }

    private fun shouldIncreaseRiseSpeed() = allowSpeedUp &&
        megaman.body.getBounds().getY() > body.getBounds().getMaxY() + DIST_TRIGGER_SPEED_UP * ConstVals.PPM

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
