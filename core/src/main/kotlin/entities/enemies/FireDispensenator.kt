package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
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
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
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
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.EnemyDamageNegotiations
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.*

class FireDispensenator(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "FireDispensenator"
        private const val OPEN_DUR = 0.25f
        private const val CLOSE_DUR = 0.25f
        private const val FIRE_TIME = 0.4f
        private const val FIRE_DUR = 0.5f
        private const val SLEEP_DUR = 0.25f
        private const val FIRE_TRAJ_X = 8f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class FireDispensenatorState { OPEN, CLOSE, FIRE, SLEEP }

    override val damageNegotiations = EnemyDamageNegotiations.getEnemyDmgNegs(Size.MEDIUM)
    override lateinit var facing: Facing

    private val timers = objectMapOf(
        "open" pairTo Timer(OPEN_DUR),
        "fire" pairTo Timer(FIRE_DUR, gdxArrayOf(TimeMarkedRunnable(FIRE_TIME) { fire() })),
        "close" pairTo Timer(CLOSE_DUR),
        "sleep" pairTo Timer(SLEEP_DUR)
    )
    private lateinit var stateMachine: StateMachine<FireDispensenatorState>
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
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        scanner.set(spawnProps.get(ConstKeys.SCANNER, RectangleMapObject::class)!!.rectangle.toGameRectangle())

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        timers.values().forEach { it.reset() }
        stateMachine.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            val state = stateMachine.getCurrent()

            if (!state.equalsAny(FireDispensenatorState.CLOSE, FireDispensenatorState.FIRE))
                facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

            val timer = timers[state.name.lowercase()]
            timer.update(delta)
            if (timer.isFinished()) {
                val next = stateMachine.next()
                GameLogger.debug(TAG, "update(): next=$next")
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM)
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
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
        val keySupplier: () -> String = { stateMachine.getCurrent().name.lowercase() }
        val animations = objectMapOf<String, IAnimation>(
            "sleep" pairTo Animation(regions["sleep"]),
            "open" pairTo Animation(regions["open"], 2, 1, 0.1f, false),
            "fire" pairTo Animation(regions["fire"], 3, 1, 0.1f, false),
            "close" pairTo Animation(regions["close"], 2, 1, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun buildStateMachine(): StateMachine<FireDispensenatorState> {
        val builder = StateMachineBuilder<FireDispensenatorState>()
        FireDispensenatorState.entries.forEach { builder.state(it.name, it) }
        builder.setOnChangeState(this::onChangeState)
        builder.initialState(FireDispensenatorState.SLEEP.name)
            .transition(FireDispensenatorState.SLEEP.name, FireDispensenatorState.OPEN.name) {
                megaman.body.getBounds().overlaps(scanner)
            }
            .transition(FireDispensenatorState.OPEN.name, FireDispensenatorState.FIRE.name) { true }
            .transition(FireDispensenatorState.FIRE.name, FireDispensenatorState.CLOSE.name) { true }
            .transition(FireDispensenatorState.CLOSE.name, FireDispensenatorState.OPEN.name) {
                megaman.body.getBounds().overlaps(scanner)
            }
            .transition(FireDispensenatorState.CLOSE.name, FireDispensenatorState.SLEEP.name) { true }
        return builder.build()
    }

    private fun onChangeState(current: FireDispensenatorState, previous: FireDispensenatorState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")
        timers[previous.name.lowercase()].reset()
    }

    private fun fire() {
        val spawn = body.getPositionPoint(Position.BOTTOM_CENTER)
            .add(0.5f * ConstVals.PPM * facing.value, 0.1f * ConstVals.PPM)

        val fireWall = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.FIRE_WALL)!!
        fireWall.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                "${ConstKeys.BODY}_${ConstKeys.POSITION}" pairTo Position.BOTTOM_CENTER,
                ConstKeys.TRAJECTORY pairTo Vector2(FIRE_TRAJ_X * ConstVals.PPM * facing.value, 0f)
            )
        )

        requestToPlaySound(SoundAsset.ATOMIC_FIRE_SOUND, false)
    }
}
