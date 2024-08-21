package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.pathfinding.PathfinderParams
import com.engine.pathfinding.PathfindingComponent
import com.engine.updatables.UpdatablesComponent
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.pathfinding.StandardPathfinderResultConsumer

import com.megaman.maverick.game.world.*
import kotlin.reflect.KClass

class Bat(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity {

    enum class BatStatus(val region: String) {
        HANGING("Hang"), OPEN_EYES("OpenEyes"), OPEN_WINGS("OpenWings"), FLYING_TO_ATTACK("Fly"),
        FLYING_TO_RETREAT("Fly")
    }

    companion object {
        private var atlas: TextureAtlas? = null
        private const val DEBUG_PATHFINDING = false
        private const val HANG_DURATION = 1.75f
        private const val RELEASE_FROM_PERCH_DURATION = 0.25f
        private const val DEFAULT_FLY_TO_ATTACK_SPEED = 3f
        private const val DEFAULT_FLY_TO_RETREAT_SPEED = 8f
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
    private lateinit var type: String
    private lateinit var status: BatStatus
    private lateinit var animations: ObjectMap<String, IAnimation>
    private var flyToAttackSpeed = DEFAULT_FLY_TO_ATTACK_SPEED
    private var flyToRetreatSpeed = DEFAULT_FLY_TO_RETREAT_SPEED

    override fun init() {
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
        addComponent(definePathfindingComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

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
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            when (status) {
                BatStatus.HANGING -> {
                    hangTimer.update(it)
                    if (hangTimer.isFinished() || !body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) {
                        status = BatStatus.OPEN_EYES
                        hangTimer.reset()
                    }
                }

                BatStatus.OPEN_EYES, BatStatus.OPEN_WINGS -> {
                    releasePerchTimer.update(it)
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
        val params = PathfinderParams(startSupplier = { body.getCenter() },
            targetSupplier = { getMegaman().body.getTopCenterPoint() },
            allowDiagonal = { true },
            filter = { _, objs ->
                for (obj in objs) if (obj is Fixture && obj.getFixtureType() == FixtureType.BLOCK)
                    return@PathfinderParams false
                return@PathfinderParams true
            })

        val pathfindingComponent = PathfindingComponent(params, {
            StandardPathfinderResultConsumer.consume(
                it,
                body,
                body.getCenter(),
                { flyToAttackSpeed },
                body,
                stopOnTargetReached = false,
                stopOnTargetNull = false,
                shapes = if (DEBUG_PATHFINDING) game.getShapes() else null
            )
        }, { status == BatStatus.FLYING_TO_ATTACK })

        pathfindingComponent.updateIntervalTimer = Timer(0.1f)

        return pathfindingComponent
    }
}
