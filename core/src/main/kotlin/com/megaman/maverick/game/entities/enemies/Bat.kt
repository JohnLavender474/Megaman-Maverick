package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.isAny
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.MutableOrderedSet
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.pathfinding.PathfinderParams
import com.mega.game.engine.pathfinding.PathfindingComponent
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.GreenPelletBlast
import com.megaman.maverick.game.entities.utils.DynamicBodyHeuristic
import com.megaman.maverick.game.pathfinding.StandardPathfinderResultConsumer
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.extensions.toGridCoordinate
import com.megaman.maverick.game.world.body.*

class Bat(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IFreezableEntity,
    IDirectional {

    companion object {
        const val TAG = "Bat"
        private var atlas: TextureAtlas? = null
        private const val MAX_ALLOWED = 3
        private const val DEBUG_PATHFINDING = false
        private const val HANG_DURATION = 0.75f
        private const val FROZEN_DURATION = 0.5f
        private const val RELEASE_FROM_PERCH_DURATION = 0.25f
        private const val DEFAULT_FLY_TO_ATTACK_SPEED = 3f
        private const val HARD_MODE_FLY_SCALAR = 1.5f
        private const val DEFAULT_FLY_TO_RETREAT_SPEED = 8f
        private const val PATHFINDING_UPDATE_INTERVAL = 0.05f
        private const val HANG_AFTER_DAMAGED_INFLICTED = "hang_after_damage_inflicted"
    }

    private enum class BatState(val region: String) {
        HANGING("Hang"),
        OPEN_EYES("OpenEyes"),
        OPEN_WINGS("OpenWings"),
        FLYING_TO_ATTACK("Fly"),
        FLYING_TO_RETREAT("Fly")
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override var frozen: Boolean
        get() = !frozenTimer.isFinished()
        set(value) {
            if (value) frozenTimer.reset() else frozenTimer.setToEnd()
        }

    private val hangTimer = Timer(HANG_DURATION)
    private val frozenTimer = Timer(FROZEN_DURATION)
    private val releasePerchTimer = Timer(RELEASE_FROM_PERCH_DURATION)

    private val canMove: Boolean
        get() = !game.isCameraRotating() && !frozen

    private lateinit var type: String
    private lateinit var state: BatState
    private lateinit var animations: ObjectMap<String, IAnimation>

    private var flyToAttackSpeed = DEFAULT_FLY_TO_ATTACK_SPEED
    private var flyToRetreatSpeed = DEFAULT_FLY_TO_RETREAT_SPEED

    private var trigger: GameRectangle? = null
    private var triggered = false

    private val blocksToIgnore = ObjectSet<Int>()
    private val reusableBodySet = MutableOrderedSet<IBody>()

    private var hangAfterDamageInflicted = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        super.init()
        addComponent(defineAnimationsComponent())
        addComponent(definePathfindingComponent())
    }

    override fun canSpawn(spawnProps: Properties) =
        MegaGameEntities.getOfTag(TAG).size < MAX_ALLOWED && super.canSpawn(spawnProps)

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        hangTimer.reset()
        releasePerchTimer.reset()

        val state = spawnProps.getOrDefault(ConstKeys.STATE, BatState.HANGING)
        this.state = when (state) {
            is BatState -> state
            is String -> BatState.valueOf(state.uppercase())
            else -> throw IllegalArgumentException("Invalid state type: $state")
        }

        frozen = false

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.setTopCenterToPoint(bounds.getPositionPoint(Position.TOP_CENTER))

        type = spawnProps.getOrDefault(ConstKeys.TYPE, "", String::class)

        val frameDuration = spawnProps.getOrDefault(ConstKeys.FRAME, 0.1f, Float::class)
        gdxArrayOf(animations.get("Fly"), animations.get("SnowFly")).forEach { it.setFrameDuration(frameDuration) }

        flyToAttackSpeed = spawnProps.getOrDefault(
            "${ConstKeys.ATTACK}_${ConstKeys.SPEED}",
            DEFAULT_FLY_TO_ATTACK_SPEED,
            Float::class
        )
        if (game.state.getDifficultyMode() == DifficultyMode.HARD) flyToAttackSpeed *= HARD_MODE_FLY_SCALAR

        flyToRetreatSpeed = spawnProps.getOrDefault(
            "${ConstKeys.RETREAT}_${ConstKeys.SPEED}",
            DEFAULT_FLY_TO_RETREAT_SPEED,
            Float::class
        )

        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase())

        trigger = spawnProps.get(ConstKeys.TRIGGER, RectangleMapObject::class)?.rectangle?.toGameRectangle(false)
        triggered = trigger == null

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.IGNORE)) {
                val id = (value as RectangleMapObject).properties.get(ConstKeys.ID, Int::class.java)
                blocksToIgnore.add(id)
            }
        }

        hangAfterDamageInflicted = spawnProps.getOrDefault(
            HANG_AFTER_DAMAGED_INFLICTED, true, Boolean::class
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        blocksToIgnore.clear()
    }

    override fun canBeDamagedBy(damager: IDamager) =
        damager.isAny(Axe::class, Wanaan::class, GreenPelletBlast::class) || super.canBeDamagedBy(damager)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        GameLogger.debug(TAG, "takeDamageFrom(): damager=$damager")

        if (damager is Wanaan) {
            depleteHealth()
            return true
        }

        val damaged = super.takeDamageFrom(damager)
        if (damaged && type.lowercase() != ConstKeys.SNOW && !frozen && damager is IFreezerEntity) frozen = true

        return damaged
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        GameLogger.debug(TAG, "onDamageInflictedTo(): damageable=$damageable")
        if (damageable == megaman)
            state = if (hangAfterDamageInflicted) BatState.FLYING_TO_RETREAT else BatState.FLYING_TO_ATTACK
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (frozen) {
                frozenTimer.update(delta)
                if (frozenTimer.isJustFinished()) {
                    damageTimer.reset()
                    IceShard.spawn5(body.getCenter())
                    state = BatState.FLYING_TO_RETREAT
                }
                return@add
            }

            when (state) {
                BatState.HANGING -> {
                    if (!triggered) trigger?.let {
                        if (megaman.body.getBounds().overlaps(it)) {
                            GameLogger.debug(TAG, "update(): Megaman touched trigger")
                            triggered = true
                        }
                    }

                    hangTimer.update(delta)

                    if (triggered && (hangTimer.isFinished() || !body.isSensing(BodySense.HEAD_TOUCHING_BLOCK))) {
                        state = BatState.OPEN_EYES

                        GameLogger.debug(TAG, "update(): set status to $state")

                        hangTimer.reset()
                        releasePerchTimer.reset()
                    }
                }

                BatState.OPEN_EYES, BatState.OPEN_WINGS -> {
                    releasePerchTimer.update(delta)
                    if (releasePerchTimer.isFinished()) {
                        state = if (state == BatState.OPEN_EYES) BatState.OPEN_WINGS else BatState.FLYING_TO_ATTACK
                        releasePerchTimer.reset()
                    }
                }

                else -> {}
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
        val hitHeadOnBlock: (Block) -> Unit = hit@{ block ->
            if (state != BatState.FLYING_TO_RETREAT) return@hit

            val id = block.id
            if (blocksToIgnore.contains(id)) return@hit

            state = BatState.HANGING
        }
        headFixture.setHitByBlockReceiver(ProcessState.BEGIN) { block, _ -> hitHeadOnBlock.invoke(block) }
        headFixture.setHitByBlockReceiver(ProcessState.CONTINUE) { block, _ -> hitHeadOnBlock.invoke(block) }
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle(body))
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            shieldFixture.setActive(frozen || state == BatState.HANGING)
            damageableFixture.setActive(!frozen && state != BatState.HANGING)

            when {
                !canMove -> body.physics.velocity.setZero()

                state == BatState.FLYING_TO_RETREAT -> {
                    val velocity = GameObjectPools.fetch(Vector2::class)
                    when (direction) {
                        Direction.UP -> velocity.set(0f, flyToRetreatSpeed)
                        Direction.DOWN -> velocity.set(0f, -flyToRetreatSpeed)
                        Direction.LEFT -> velocity.set(-flyToAttackSpeed, 0f)
                        Direction.RIGHT -> velocity.set(flyToRetreatSpeed, 0f)
                    }.scl(ConstVals.PPM.toFloat())
                    body.physics.velocity.set(velocity)
                }

                state != BatState.FLYING_TO_ATTACK -> body.physics.velocity.setZero()
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.hidden = damageBlink
            sprite.setOriginCenter()
            sprite.rotation = megaman.direction.rotation
            sprite.setCenter(body.getCenter())
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { type + if (frozen) "frozen" else state.region }
        animations = objectMapOf(
            "Hang" pairTo Animation(atlas!!.findRegion("Bat/Hang")),
            "Fly" pairTo Animation(atlas!!.findRegion("Bat/Fly"), 1, 2, 0.1f, true),
            "OpenEyes" pairTo Animation(atlas!!.findRegion("Bat/OpenEyes")),
            "OpenWings" pairTo Animation(atlas!!.findRegion("Bat/OpenWings")),
            "SnowHang" pairTo Animation(atlas!!.findRegion("SnowBat/Hang")),
            "frozen" pairTo Animation(atlas!!.findRegion("$TAG/frozen")),
            "SnowFly" pairTo Animation(atlas!!.findRegion("SnowBat/Fly"), 1, 2, 0.1f, true),
            "SnowOpenEyes" pairTo Animation(atlas!!.findRegion("SnowBat/OpenEyes")),
            "SnowOpenWings" pairTo Animation(atlas!!.findRegion("SnowBat/OpenWings"))
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun definePathfindingComponent(): PathfindingComponent {
        val params = PathfinderParams(
            startCoordinateSupplier = { body.getCenter().toGridCoordinate() },
            targetCoordinateSupplier = { megaman.body.getCenter().toGridCoordinate() },
            allowDiagonal = { true },
            filter = filter@{ coordinate ->
                game.getWorldContainer()!!.getBodies(coordinate.x, coordinate.y, reusableBodySet)

                var passable = true

                for (otherBody in reusableBodySet) if (otherBody.getEntity().getType() == EntityType.BLOCK) {
                    passable = false
                    break
                }

                reusableBodySet.clear()

                return@filter passable
            },
            properties = props(ConstKeys.HEURISTIC pairTo DynamicBodyHeuristic(game))
        )
        val pathfindingComponent = PathfindingComponent(
            params,
            {
                if (canMove) StandardPathfinderResultConsumer.consume(
                    result = it,
                    body = body,
                    start = body.getCenter(),
                    speed = { flyToAttackSpeed * ConstVals.PPM },
                    stopOnTargetReached = false,
                    onTargetNull = { directlyChaseMegaman() },
                    stopOnTargetNull = false,
                    shapes = if (DEBUG_PATHFINDING) game.getShapes() else null
                )
            },
            { canMove && state == BatState.FLYING_TO_ATTACK },
            intervalTimer = Timer(PATHFINDING_UPDATE_INTERVAL)
        )
        return pathfindingComponent
    }

    private fun directlyChaseMegaman() = body.physics.velocity.set(
        megaman.body
            .getCenter()
            .sub(body.getCenter())
            .nor()
            .scl(flyToAttackSpeed * ConstVals.PPM)
    )
}
