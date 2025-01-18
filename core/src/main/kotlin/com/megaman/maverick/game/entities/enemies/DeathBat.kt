package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.pathfinding.PathfinderParams
import com.mega.game.engine.pathfinding.PathfindingComponent
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.utils.DynamicBodyHeuristic
import com.megaman.maverick.game.pathfinding.StandardPathfinderResultConsumer
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.extensions.toGridCoordinate
import com.megaman.maverick.game.world.body.*

class DeathBat(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity {

    companion object {
        const val TAG = "DeathBat"

        private const val HANG_DUR = 1f
        private const val OPEN_DUR = 0.3f

        private const val CACA_FLAME_DELAY = 1f

        private const val ATTACK_SPEED = 3f
        private const val RETREAT_SPEED = 8f

        private const val PATHFINDING_UPDATE_INTERVAL = 0.05f

        private val animDefs = ObjectMap<DeathBatState, AnimationDef>()
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class DeathBatState { HANG, OPEN, FLY_ATTACK, FLY_RETREAT }

    private val loop = Loop(DeathBatState.entries.toGdxArray())
    private val currentState: DeathBatState
        get() = loop.getCurrent()

    private val stateTimers = OrderedMap<DeathBatState, Timer>()
    private val cacaFlameDelay = Timer(CACA_FLAME_DELAY)

    private var trigger: GameRectangle? = null
    private var triggered = false

    override fun init() {
        GameLogger.debug(TAG, "init()")

        damageOverrides.put(Fireball::class, null)

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            DeathBatState.entries.forEach {
                val key = it.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }

        if (animDefs.isEmpty) {
            animDefs.put(DeathBatState.HANG, AnimationDef())
            animDefs.put(DeathBatState.OPEN, AnimationDef(3, 1, 0.1f, false))
            animDefs.put(DeathBatState.FLY_ATTACK, AnimationDef(2, 1, 0.1f, true))
            animDefs.put(DeathBatState.FLY_RETREAT, AnimationDef(2, 1, 0.1f, true))
        }

        if (stateTimers.isEmpty) {
            stateTimers.put(DeathBatState.HANG, Timer(HANG_DUR))
            stateTimers.put(DeathBatState.OPEN, Timer(OPEN_DUR))
        }

        super.init()

        addComponent(definePathfindingComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.TOP_CENTER)
        body.setTopCenterToPoint(spawn)

        trigger = spawnProps.get(ConstKeys.TRIGGER, RectangleMapObject::class)?.rectangle?.toGameRectangle(false)
        triggered = trigger == null

        loop.reset()
        stateTimers.values().forEach { it.reset() }

        cacaFlameDelay.setToEnd()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        if (currentState == DeathBatState.FLY_ATTACK && damageable == megaman) loop.next()
    }

    private fun spawnCacaFlame() {
        GameLogger.debug(TAG, "spawnCacaFlame()")

        val flame = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.CACA_FLAME)!!
        flame.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (currentState) {
                DeathBatState.HANG -> {
                    body.physics.velocity.setZero()

                    if (!triggered) trigger?.let {
                        if (megaman.body.getBounds().overlaps(it)) {
                            GameLogger.debug(TAG, "update(): Megaman touched trigger")
                            triggered = true
                        }
                    }

                    val timer = stateTimers[DeathBatState.HANG]
                    timer.update(delta)

                    if (triggered && timer.isFinished()) {
                        loop.next()
                        timer.reset()
                    }
                }

                DeathBatState.OPEN -> {
                    body.physics.velocity.setZero()

                    val timer = stateTimers[DeathBatState.OPEN]
                    timer.update(delta)

                    if (triggered && timer.isFinished()) {
                        loop.next()
                        timer.reset()
                    }
                }

                DeathBatState.FLY_ATTACK -> {
                    cacaFlameDelay.update(delta)

                    if (!cacaFlameDelay.isFinished()) return@add

                    val centerX = body.getCenter().x

                    if (!body.isSensing(BodySense.BODY_TOUCHING_BLOCK) &&
                        body.getY() > megaman.body.getMaxY() &&
                        centerX >= megaman.body.getX() &&
                        centerX <= megaman.body.getMaxX()
                    ) {
                        spawnCacaFlame()

                        cacaFlameDelay.reset()
                    }
                }

                DeathBatState.FLY_RETREAT -> {
                    body.physics.velocity.set(0f, -RETREAT_SPEED * ConstVals.PPM)

                    if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) loop.next()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val headFixture = Fixture(
            body, FixtureType.HEAD, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.175f * ConstVals.PPM)
        )
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle(body))
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            shieldFixture.setActive(currentState == DeathBatState.HANG)
            damageableFixture.setActive(currentState != DeathBatState.HANG)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
                .also { sprite -> sprite.setSize(3f * ConstVals.PPM, 2f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.hidden = damageBlink
            sprite.setPosition(body.getCenter(), Position.CENTER)
        }
        .build()

    private fun definePathfindingComponent(): PathfindingComponent {
        val params = PathfinderParams(
            startCoordinateSupplier = { body.getCenter().toGridCoordinate() },
            targetCoordinateSupplier = { megaman.body.getCenter().toGridCoordinate() },
            allowDiagonal = { true },
            filter = { coordinate ->
                val bodies = game.getWorldContainer()!!.getBodies(coordinate.x, coordinate.y)
                var passable = true
                var blockingBody: IBody? = null

                for (otherBody in bodies) if (otherBody.getEntity().getEntityType() == EntityType.BLOCK) {
                    passable = false
                    blockingBody = otherBody
                    break
                }

                passable
            },
            properties = props(ConstKeys.HEURISTIC pairTo DynamicBodyHeuristic(game))
        )
        val pathfindingComponent = PathfindingComponent(
            params = params,
            consumer = {
                StandardPathfinderResultConsumer.consume(
                    result = it,
                    body = body,
                    start = body.getCenter(),
                    speed = { ATTACK_SPEED * ConstVals.PPM },
                    stopOnTargetReached = false,
                    onTargetNull = { directlyChaseMegaman() },
                    stopOnTargetNull = false
                )
            },
            doUpdate = { currentState == DeathBatState.FLY_ATTACK },
            intervalTimer = Timer(PATHFINDING_UPDATE_INTERVAL)
        )
        return pathfindingComponent
    }

    private fun directlyChaseMegaman() = body.physics.velocity.set(
        megaman.body
            .getCenter()
            .sub(body.getCenter())
            .nor()
            .scl(ATTACK_SPEED * ConstVals.PPM)
    )

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { currentState.name.lowercase() }
                .applyToAnimations { animations ->
                    animDefs.forEach {
                        val key = it.key.name.lowercase()
                        val region = regions[key]
                        val def = it.value
                        animations.put(key, Animation(region, def.rows, def.cols, def.durations, def.loop))
                    }
                }
                .build()
        )
        .build()
}
