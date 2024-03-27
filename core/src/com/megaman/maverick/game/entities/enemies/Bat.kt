package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
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
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.*
import kotlin.reflect.KClass

class Bat(game: MegamanMaverickGame) : AbstractEnemy(game) {

    enum class BatStatus(val region: String) {
        HANGING("Hang"), OPEN_EYES("OpenEyes"), OPEN_WINGS("OpenWings"), FLYING_TO_ATTACK("Fly"),
        FLYING_TO_RETREAT("Fly")
    }

    companion object {
        private var atlas: TextureAtlas? = null

        private const val DEBUG_PATHFINDING = false
        private const val HANG_DURATION = 1.75f
        private const val RELEASE_FROM_PERCH_DURATION = .25f
        private const val FLY_TO_ATTACK_SPEED = 3f
        private const val FLY_TO_RETREAT_SPEED = 8f
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10), Fireball::class to dmgNeg(ConstVals.MAX_HEALTH), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }, ChargedShotExplosion::class to dmgNeg(ConstVals.MAX_HEALTH)
    )

    private val hangTimer = Timer(HANG_DURATION)
    private val releasePerchTimer = Timer(RELEASE_FROM_PERCH_DURATION)

    private lateinit var type: String
    private lateinit var status: BatStatus

    override fun init() {
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        super.init()

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

        type = if (spawnProps.containsKey(ConstKeys.TYPE)) spawnProps.get(ConstKeys.TYPE) as String else ""
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)

        updatablesComponent.add {
            when (status) {
                BatStatus.HANGING -> {
                    hangTimer.update(it)
                    if (hangTimer.isFinished()) {
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

                BatStatus.FLYING_TO_ATTACK -> { // TODO: add attack logic
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(.5f * ConstVals.PPM, .25f * ConstVals.PPM)

        // head fixture
        val headFixture = Fixture(
            body, FixtureType.HEAD, GameRectangle().setSize(.5f * ConstVals.PPM, .175f * ConstVals.PPM)
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

        val scannerFixture = Fixture(body, FixtureType.CONSUMER, model.copy())
        val consumer: (IFixture) -> Unit = {
            if (it.getFixtureType() == FixtureType.DAMAGEABLE && it.getEntity() == getMegamanMaverickGame().megaman) status =
                BatStatus.FLYING_TO_RETREAT
        }
        scannerFixture.setConsumer { _, it -> consumer(it) }
        body.addFixture(scannerFixture)

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            shieldFixture.active = status == BatStatus.HANGING
            damageableFixture.active = status != BatStatus.HANGING

            if (status == BatStatus.FLYING_TO_RETREAT) body.physics.velocity.set(
                0f, FLY_TO_RETREAT_SPEED * ConstVals.PPM
            )
            else if (status != BatStatus.FLYING_TO_ATTACK) body.physics.velocity.setZero()
        })

        addComponent(
            DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true)
        )

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)

        val SpritesComponent = SpritesComponent(this, "bat" to sprite)
        SpritesComponent.putUpdateFunction("bat") { _, _sprite ->
            _sprite as GameSprite
            _sprite.setPosition(body.getCenter(), Position.CENTER)
        }
        return SpritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = { type + status.region }
        val animator = Animator(
            keySupplier, objectMapOf(
                "Hang" to Animation(atlas!!.findRegion("Bat/Hang"), true),
                "Fly" to Animation(atlas!!.findRegion("Bat/Fly"), 1, 2, 0.1f, true),
                "OpenEyes" to Animation(atlas!!.findRegion("Bat/OpenEyes"), true),
                "OpenWings" to Animation(atlas!!.findRegion("Bat/OpenWings"), true),
                "SnowHang" to Animation(atlas!!.findRegion("SnowBat/Hang"), true),
                "SnowFly" to Animation(atlas!!.findRegion("SnowBat/Fly"), 1, 2, 0.1f, true),
                "SnowOpenEyes" to Animation(atlas!!.findRegion("SnowBat/OpenEyes"), true),
                "SnowOpenWings" to Animation(atlas!!.findRegion("SnowBat/OpenWings"), true)
            )
        )
        return AnimationsComponent(this, animator)
    }

    private fun definePathfindingComponent(): PathfindingComponent {
        val params = PathfinderParams(startSupplier = { body.getCenter() },
            targetSupplier = { getMegamanMaverickGame().megaman.body.getTopCenterPoint() },
            allowDiagonal = { true },
            filter = { _, objs ->
                for (obj in objs) if (obj is Fixture && obj.getFixtureType() == FixtureType.BLOCK) return@PathfinderParams false
                return@PathfinderParams true
            })

        val pathfindingComponent = PathfindingComponent(this, params, {
            StandardPathfinderResultConsumer.consume(
                it,
                body,
                body.getCenter(),
                FLY_TO_ATTACK_SPEED,
                body,
                stopOnTargetReached = false,
                stopOnTargetNull = false,
                shapes = if (DEBUG_PATHFINDING) getMegamanMaverickGame().getShapes() else null
            )
        }, { status == BatStatus.FLYING_TO_ATTACK })

        pathfindingComponent.updateIntervalTimer = Timer(0.1f)

        return pathfindingComponent
    }
}
