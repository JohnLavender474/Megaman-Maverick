package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.state.StateMachine
import com.mega.game.engine.state.StateMachineBuilder
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.projectiles.FireWall
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.*

class FireDispensenator(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity,
    IFaceable {

    companion object {
        const val TAG = "FireDispensenator"
        private const val OPEN_DUR = 0.25f
        private const val CLOSE_DUR = 0.25f
        private const val FIRE_TIME = 0.4f
        private const val FIRE_DUR = 1f
        private const val SLEEP_DUR = 0.25f
        private const val FROZEN_DUR = 1f
        private const val FIRE_TRAJ_X = 8f
        private const val GROUND_GRAVITY = -0.01f
        private const val GRAVITY = -0.15f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class FireDispensenatorState { OPEN, CLOSE, FIRE, SLEEP, FROZEN }

    override lateinit var facing: Facing

    private val timers = objectMapOf(
        "open" pairTo Timer(OPEN_DUR),
        "fire" pairTo Timer(FIRE_DUR, gdxArrayOf(TimeMarkedRunnable(FIRE_TIME) { fire() })),
        "close" pairTo Timer(CLOSE_DUR),
        "sleep" pairTo Timer(SLEEP_DUR),
        "frozen" pairTo Timer(FROZEN_DUR)
    )
    private lateinit var stateMachine: StateMachine<FireDispensenatorState>
    private val currentState: FireDispensenatorState
        get() = stateMachine.getCurrentElement()
    private val shouldBeFrozen: Boolean
        get() = !timers["frozen"].isFinished()
    private val ignoreBlockSet = ObjectSet<Int>()
    private val scanner = GameRectangle()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            FireDispensenatorState.entries.forEach {
                val key = it.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
        damageOverrides.put(SmallIceCube::class, dmgNeg(15))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        scanner.set(spawnProps.get(ConstKeys.SCANNER, RectangleMapObject::class)!!.rectangle.toGameRectangle())

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        timers.forEach { entry ->
            val key = entry.key
            val timer = entry.value
            if (key == "frozen") timer.setToEnd() else timer.reset()
        }
        stateMachine.reset()

        if (spawnProps.containsKey(FireWall.IGNORE_BLOCKS)) {
            val ignoreBlocks = spawnProps.get(FireWall.IGNORE_BLOCKS, String::class)!!.split(",")
            ignoreBlocks.forEach { id -> ignoreBlockSet.add(id.toInt()) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ignoreBlockSet.clear()
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damaged = super.takeDamageFrom(damager)
        if (damaged && currentState != FireDispensenatorState.FROZEN && damager is IFreezerEntity) onFrozen()
        return damaged
    }

    private fun onFrozen() = timers["frozen"].reset()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            val state = stateMachine.getCurrentElement()

            if (shouldBeFrozen && state != FireDispensenatorState.FROZEN) stateMachine.next()

            if (!state.equalsAny(
                    FireDispensenatorState.CLOSE,
                    FireDispensenatorState.FROZEN,
                    FireDispensenatorState.FIRE
                )
            ) facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

            val timer = timers[state.name.lowercase()]
            timer.update(delta)
            if (timer.isFinished()) {
                val next = stateMachine.next()
                GameLogger.debug(TAG, "update(): next=$next")
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(2f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(1.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -ConstVals.PPM.toFloat()
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.GRAVITY) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(3f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { stateMachine.getCurrentElement().name.lowercase() }
        val animations = objectMapOf<String, IAnimation>(
            "sleep" pairTo Animation(regions["sleep"]),
            "open" pairTo Animation(regions["open"], 2, 1, 0.1f, false),
            "fire" pairTo Animation(regions["fire"], 4, 2, 0.1f, false),
            "close" pairTo Animation(regions["close"], 2, 1, 0.1f, false),
            "frozen" pairTo Animation(regions["frozen"])
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun buildStateMachine(): StateMachine<FireDispensenatorState> {
        val builder = StateMachineBuilder<FireDispensenatorState>()
        FireDispensenatorState.entries.forEach { builder.state(it.name, it) }
        builder.setOnChangeState(this::onChangeState)
        builder.initialState(FireDispensenatorState.SLEEP.name)
            // sleep
            .transition(FireDispensenatorState.SLEEP.name, FireDispensenatorState.FROZEN.name) { shouldBeFrozen }
            .transition(FireDispensenatorState.SLEEP.name, FireDispensenatorState.OPEN.name) {
                megaman.body.getBounds().overlaps(scanner)
            }
            // open
            .transition(FireDispensenatorState.OPEN.name, FireDispensenatorState.FROZEN.name) { shouldBeFrozen }
            .transition(FireDispensenatorState.OPEN.name, FireDispensenatorState.FIRE.name) { true }
            // fire
            .transition(FireDispensenatorState.FIRE.name, FireDispensenatorState.FROZEN.name) { shouldBeFrozen }
            .transition(FireDispensenatorState.FIRE.name, FireDispensenatorState.CLOSE.name) { true }
            // close
            .transition(FireDispensenatorState.CLOSE.name, FireDispensenatorState.FROZEN.name) { shouldBeFrozen }
            .transition(FireDispensenatorState.CLOSE.name, FireDispensenatorState.OPEN.name) {
                megaman.body.getBounds().overlaps(scanner)
            }
            .transition(FireDispensenatorState.CLOSE.name, FireDispensenatorState.SLEEP.name) { true }
            // frozen
            .transition(FireDispensenatorState.FROZEN.name, FireDispensenatorState.OPEN.name) { true }
        return builder.build()
    }

    private fun onChangeState(current: FireDispensenatorState, previous: FireDispensenatorState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        when (previous) {
            FireDispensenatorState.FROZEN -> {
                damageTimer.reset()

                for (i in 0 until 5) {
                    val shard = MegaEntityFactory.fetch(IceShard::class)!!
                    shard.spawn(props(ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.INDEX pairTo i))
                }
            }

            else -> timers[previous.name.lowercase()].reset()
        }
    }

    private fun fire() {
        val spawn = body.getPositionPoint(Position.BOTTOM_CENTER)
            .add(0.5f * ConstVals.PPM * facing.value, -0.25f * ConstVals.PPM)

        val fireWall = MegaEntityFactory.fetch(FireWall::class)!!
        fireWall.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                FireWall.IGNORE_BLOCKS pairTo ignoreBlockSet,
                "${ConstKeys.BODY}_${ConstKeys.POSITION}" pairTo Position.BOTTOM_CENTER,
                ConstKeys.TRAJECTORY pairTo Vector2(FIRE_TRAJ_X * ConstVals.PPM * facing.value, 0f)
            )
        )

        requestToPlaySound(SoundAsset.ATOMIC_FIRE_SOUND, false)
    }
}
