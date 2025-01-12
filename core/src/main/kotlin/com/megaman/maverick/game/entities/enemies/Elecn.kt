package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class Elecn(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    enum class ElecnState {
        MOVING, CHARGING, SHOCKING
    }

    companion object {
        const val TAG = "Elecn"
        private var atlas: TextureAtlas? = null
        private const val MOVING_DURATION = 0.5f
        private const val CHARGING_DURATION = 1f
        private const val SHOCKING_DURATION = 0.15f
        private const val ZIG_ZAG_DURATION = 0.5f
        private const val X_VEL = 2f
        private const val Y_VEL = 0.2f
        private const val SHOCK_VEL = 10f
    }

    override var facing = Facing.LEFT

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(10),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }, ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 15 else 10
        }
    )

    private val elecnLoop = Loop(ElecnState.values().toGdxArray(), false)
    private val elecnTimers = objectMapOf(
        ElecnState.MOVING pairTo Timer(MOVING_DURATION),
        ElecnState.CHARGING pairTo Timer(CHARGING_DURATION),
        ElecnState.SHOCKING pairTo Timer(SHOCKING_DURATION)
    )
    private val elecnTimer: Timer
        get() = elecnTimers[elecnLoop.getCurrent()]!!

    private val zigzagTimer = Timer(ZIG_ZAG_DURATION)

    private var zigzagUp = false

    override fun init() {
        super.init()
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        elecnLoop.reset()
        elecnTimer.reset()
        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            if (elecnLoop.getCurrent() != ElecnState.SHOCKING) {
                zigzagTimer.update(it)
                if (zigzagTimer.isFinished()) {
                    zigzagUp = !zigzagUp
                    zigzagTimer.reset()
                }
            }

            elecnTimer.update(it)
            if (elecnTimer.isFinished()) {
                val previous = elecnLoop.getCurrent()
                val elecnState = elecnLoop.next()
                GameLogger.debug(TAG, "Setting state from $previous to $elecnState")
                if (elecnState == ElecnState.SHOCKING) shock()
                elecnTimer.reset()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.85f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(
            body,
            FixtureType.BODY, GameRectangle().setSize(0.85f * ConstVals.PPM)
        )
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.85f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(0.85f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        val sideFixture = Fixture(
            body,
            FixtureType.SIDE,
            GameRectangle().setSize(0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        )
        body.addFixture(sideFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (isFacing(Facing.LEFT)) {
                sideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
                sideFixture.offsetFromBodyAttachment.x = -0.5f * ConstVals.PPM
            } else {
                sideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
                sideFixture.offsetFromBodyAttachment.x = 0.5f * ConstVals.PPM
            }

            val velocity = GameObjectPools.fetch(Vector2::class)
            when (ElecnState.SHOCKING) {
                elecnLoop.getCurrent() -> velocity.setZero()
                else -> {
                    val x = X_VEL * ConstVals.PPM * facing.value
                    val y = Y_VEL * ConstVals.PPM * (if (zigzagUp) 1 else -1)
                    velocity.set(x, y)
                }
            }
            body.physics.velocity.set(velocity)
        }

        body.postProcess.put(ConstKeys.DEFAULT) {
            if (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) facing = Facing.RIGHT
            else if (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) facing = Facing.LEFT
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (elecnLoop.getCurrent()) {
                ElecnState.MOVING -> "moving"
                ElecnState.CHARGING -> "charging"
                ElecnState.SHOCKING -> "shocking"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "moving" pairTo Animation(atlas!!.findRegion("Elecn/Elecn1")),
            "charging" pairTo Animation(atlas!!.findRegion("Elecn/Elecn2"), 1, 2, 0.15f, true),
            "shocking" pairTo Animation(atlas!!.findRegion("Elecn/Elecn3"))
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun shock() {
        requestToPlaySound(SoundAsset.MM3_ELECTRIC_PULSE_SOUND, false)
        Position.entries.forEach {
            if (it == Position.CENTER) return@forEach

            val xVel = ConstVals.PPM * (if (it.x == 1) 0f else if (it.x > 1) SHOCK_VEL else -SHOCK_VEL)
            val yVel = ConstVals.PPM * (if (it.y == 1) 0f else if (it.y > 1) SHOCK_VEL else -SHOCK_VEL)

            val shock = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.ELECTRIC_BALL)!!
            shock.spawn(
                props(
                    ConstKeys.POSITION pairTo body.getPositionPoint(Position.TOP_CENTER),
                    ConstKeys.X pairTo xVel,
                    ConstKeys.Y pairTo yVel
                )
            )
        }
    }
}
