package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toObjectSet
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.pathfinding.PathfinderParams
import com.mega.game.engine.pathfinding.PathfindingComponent
import com.mega.game.engine.pathfinding.heuristics.EuclideanHeuristic
import com.mega.game.engine.pathfinding.heuristics.IHeuristic
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.pathfinding.StandardPathfinderResultConsumer
import com.megaman.maverick.game.utils.isNeighborOf
import com.megaman.maverick.game.utils.toGridCoordinate
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class Bat(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity {

    enum class BatStatus(val region: String) {
        HANGING("Hang"), OPEN_EYES("OpenEyes"), OPEN_WINGS("OpenWings"), FLYING_TO_ATTACK("Fly"),
        FLYING_TO_RETREAT("Fly")
    }

    companion object {
        const val TAG = "Bat"
        private var atlas: TextureAtlas? = null
        private const val DEBUG_PATHFINDING = true
        private const val DEBUG_PATHFINDING_DURATION = 1f
        private const val HANG_DURATION = 1.75f
        private const val RELEASE_FROM_PERCH_DURATION = 0.25f
        private const val DEFAULT_FLY_TO_ATTACK_SPEED = 3f
        private const val DEFAULT_FLY_TO_RETREAT_SPEED = 8f
        private const val PATHFINDING_UPDATE_INTERVAL = 0.05f
    }

    private class BatHeuristic(private val game: MegamanMaverickGame) : IHeuristic {

        companion object {
            private const val CONTAINS_BLOCK_SCALAR = 5
        }

        private val defaultHeuristic = EuclideanHeuristic()

        private fun containsBlock(x: Int, y: Int): Boolean {
            val bodies = game.getWorldContainer().getBodies(x, y)
            for (body in bodies) if (body.getEntity().getEntityType() == EntityType.BLOCK) return true
            return false
        }

        override fun calculate(x1: Int, y1: Int, x2: Int, y2: Int): Int {
            var cost = defaultHeuristic.calculate(x1, y1, x2, y2)
            if (containsBlock(x2, y2)) cost *= CONTAINS_BLOCK_SCALAR
            return cost
        }
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(15),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        })

    private val hangTimer = Timer(HANG_DURATION)
    private val releasePerchTimer = Timer(RELEASE_FROM_PERCH_DURATION)
    private val debugPathfindingTimer = Timer(DEBUG_PATHFINDING_DURATION)
    private lateinit var type: String
    private lateinit var status: BatStatus
    private lateinit var animations: ObjectMap<String, IAnimation>
    private var flyToAttackSpeed = DEFAULT_FLY_TO_ATTACK_SPEED
    private var flyToRetreatSpeed = DEFAULT_FLY_TO_RETREAT_SPEED

    @Volatile
    private var printDebugFilter = false

    override fun init() {
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        super.init()
        addComponent(defineAnimationsComponent())
        addComponent(definePathfindingComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        hangTimer.reset()
        releasePerchTimer.reset()
        status = BatStatus.HANGING

        val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
        body.setTopCenterToPoint(bounds.getTopCenterPoint())

        type = spawnProps.getOrDefault(ConstKeys.TYPE, "", String::class)

        val frameDuration = spawnProps.getOrDefault(ConstKeys.FRAME, 0.1f, Float::class)
        gdxArrayOf(animations.get("Fly"), animations.get("SnowFly")).forEach { it.setFrameDuration(frameDuration) }

        flyToAttackSpeed = spawnProps.getOrDefault(
            "${ConstKeys.ATTACK}_${ConstKeys.SPEED}", DEFAULT_FLY_TO_ATTACK_SPEED, Float::class
        )
        flyToRetreatSpeed = spawnProps.getOrDefault(
            "${ConstKeys.RETREAT}_${ConstKeys.SPEED}", DEFAULT_FLY_TO_RETREAT_SPEED, Float::class
        )

        debugPathfindingTimer.reset()
        printDebugFilter = DEBUG_PATHFINDING
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (DEBUG_PATHFINDING) {
                debugPathfindingTimer.update(delta)
                if (debugPathfindingTimer.isFinished()) {
                    printDebugFilter = true
                    val coordinate = body.getCenter().toGridCoordinate()
                    val surroundingEntityTypes = OrderedMap<IntPair, ObjectSet<EntityType>>()
                    for (i in -1..1) {
                        for (j in -1..1) {
                            val entityTypes = game.getWorldContainer().getBodies(coordinate.x + i, coordinate.y + j)
                                .map { body -> body.getEntity().getEntityType() }.toObjectSet()
                            surroundingEntityTypes.put(IntPair(coordinate.x + i, coordinate.y + j), entityTypes)
                        }
                    }
                    GameLogger.debug(TAG, "Current coordinate: $coordinate")
                    GameLogger.debug(TAG, "Surrounding coordinates: $surroundingEntityTypes")
                    debugPathfindingTimer.reset()
                }
            }

            when (status) {
                BatStatus.HANGING -> {
                    hangTimer.update(delta)
                    if (hangTimer.isFinished() || !body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) {
                        status = BatStatus.OPEN_EYES
                        hangTimer.reset()
                    }
                }

                BatStatus.OPEN_EYES, BatStatus.OPEN_WINGS -> {
                    releasePerchTimer.update(delta)
                    if (releasePerchTimer.isFinished()) {
                        if (status == BatStatus.OPEN_EYES) {
                            status = BatStatus.OPEN_WINGS
                            releasePerchTimer.reset()
                        } else status = BatStatus.FLYING_TO_ATTACK
                    }
                }

                BatStatus.FLYING_TO_RETREAT -> {
                    if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) status = BatStatus.HANGING
                }

                else -> {}
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(.5f * ConstVals.PPM, .25f * ConstVals.PPM)

        val headFixture = Fixture(
            body, FixtureType.HEAD, GameRectangle().setSize(0.5f * ConstVals.PPM, .175f * ConstVals.PPM)
        )
        headFixture.offsetFromBodyCenter.y = 0.375f * ConstVals.PPM
        body.addFixture(headFixture)

        val model = GameRectangle().setSize(0.75f * ConstVals.PPM)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, model.copy())
        body.addFixture(damageableFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, model.copy())
        body.addFixture(damagerFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, model.copy())
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)

        val scannerFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.7f * ConstVals.PPM))
        val consumer: (IFixture) -> Unit = {
            if (it.getFixtureType() == FixtureType.DAMAGEABLE && it.getEntity() == getMegaman()) status =
                BatStatus.FLYING_TO_RETREAT
        }
        scannerFixture.setConsumer { _, it -> consumer(it) }
        body.addFixture(scannerFixture)

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            shieldFixture.active = status == BatStatus.HANGING
            damageableFixture.active = status != BatStatus.HANGING

            if (status == BatStatus.FLYING_TO_RETREAT) body.physics.velocity.set(0f, flyToRetreatSpeed * ConstVals.PPM)
            else if (status != BatStatus.FLYING_TO_ATTACK) body.physics.velocity.setZero()
        })

        addComponent(
            DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true)
        )

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setPosition(body.getCenter(), Position.CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = { type + status.region }
        animations = objectMapOf(
            "Hang" to Animation(atlas!!.findRegion("Bat/Hang")),
            "Fly" to Animation(atlas!!.findRegion("Bat/Fly"), 1, 2, 0.1f, true),
            "OpenEyes" to Animation(atlas!!.findRegion("Bat/OpenEyes")),
            "OpenWings" to Animation(atlas!!.findRegion("Bat/OpenWings")),
            "SnowHang" to Animation(atlas!!.findRegion("SnowBat/Hang")),
            "SnowFly" to Animation(atlas!!.findRegion("SnowBat/Fly"), 1, 2, 0.1f, true),
            "SnowOpenEyes" to Animation(atlas!!.findRegion("SnowBat/OpenEyes")),
            "SnowOpenWings" to Animation(atlas!!.findRegion("SnowBat/OpenWings"))
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun definePathfindingComponent(): PathfindingComponent {
        val params = PathfinderParams(
            startCoordinateSupplier = { body.getCenter().toGridCoordinate() },
            targetCoordinateSupplier = { getMegaman().body.getTopCenterPoint().toGridCoordinate() },
            allowDiagonal = { true },
            filter = { coordinate ->
                val bodies = game.getWorldContainer().getBodies(coordinate.x, coordinate.y)
                var passable = true
                var blockingBody: Body? = null

                for (otherBody in bodies) if (otherBody.getEntity().getEntityType() == EntityType.BLOCK) {
                    passable = false
                    blockingBody = otherBody
                    break
                }

                if (!passable && coordinate.isNeighborOf(
                        body.getCenter().toGridCoordinate()
                    )
                ) blockingBody?.let { passable = !body.overlaps(it as Rectangle) }

                if (printDebugFilter) {
                    GameLogger.debug(TAG, "Can pass $coordinate: $passable")
                    printDebugFilter = false
                }

                passable
            },
            properties = props(ConstKeys.HEURISTIC to BatHeuristic(game))
        )
        val pathfindingComponent = PathfindingComponent(params, {
            StandardPathfinderResultConsumer.consume(
                it,
                body,
                body.getCenter(),
                { flyToAttackSpeed * ConstVals.PPM },
                body,
                stopOnTargetReached = false,
                stopOnTargetNull = false,
                shapes = if (DEBUG_PATHFINDING) game.getShapes() else null
            )
        }, { status == BatStatus.FLYING_TO_ATTACK }, intervalTimer = Timer(PATHFINDING_UPDATE_INTERVAL)
        )
        return pathfindingComponent
    }
}
