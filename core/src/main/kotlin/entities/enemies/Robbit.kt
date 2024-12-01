package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureAtlas
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
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
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
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getPositionPoint
import com.megaman.maverick.game.world.body.isSensing
import kotlin.reflect.KClass

class Robbit(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        const val TAG = "Robbit"
        private var atlas: TextureAtlas? = null
        private const val STAND_DUR = 0.75f
        private const val CROUCH_DUR = 0.25f
        private const val JUMP_DUR = 0.25f
        private const val G_GRAV = -0.001f
        private const val GRAV = -0.15f
        private const val JUMP_X = 5f
        private const val JUMP_Y = 8f
    }

    private enum class RobbitState { STANDING, CROUCHING, JUMPING }

    override var facing = Facing.RIGHT
    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(10),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 15 else 10
        }
    )

    private val robbitLoop = Loop(RobbitState.values().toGdxArray())
    private val robbitTimers =
        objectMapOf(
            RobbitState.STANDING pairTo Timer(STAND_DUR),
            RobbitState.CROUCHING pairTo Timer(CROUCH_DUR),
            RobbitState.JUMPING pairTo Timer(JUMP_DUR)
        )
    private val robbitTimer: Timer
        get() = robbitTimers[robbitLoop.getCurrent()]!!

    override fun getTag() = TAG

    override fun init() {
        super.init()
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        robbitLoop.reset()
        robbitTimer.reset()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(1.5f * ConstVals.PPM))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture}

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(1.35f * ConstVals.PPM, 0.2f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -0.75f * ConstVals.PPM
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture}

        val damageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(1.5f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(1.5f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y =
                ConstVals.PPM * (if (body.isSensing(BodySense.FEET_ON_GROUND)) G_GRAV else GRAV)
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            if (robbitLoop.getCurrent() != RobbitState.JUMPING)
                facing = if (megaman().body.getX() >= body.getX()) Facing.RIGHT else Facing.LEFT

            robbitTimer.update(it)
            if (robbitTimer.isJustFinished()) {
                val currentState = robbitLoop.getCurrent()
                GameLogger.debug(TAG, "Current state: $currentState")

                val nextState = robbitLoop.next()
                GameLogger.debug(TAG, "Transitioning to state: $nextState")

                if (nextState == RobbitState.JUMPING) {
                    body.physics.velocity.x = JUMP_X * ConstVals.PPM * facing.value
                    body.physics.velocity.y = JUMP_Y * ConstVals.PPM
                }

                robbitTimer.reset()
            }
        }
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(4f * ConstVals.PPM, 3.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (!body.isSensing(BodySense.FEET_ON_GROUND)) "jump"
            else when (robbitLoop.getCurrent()) {
                RobbitState.STANDING -> "stand"
                RobbitState.CROUCHING -> "crouch"
                RobbitState.JUMPING -> "jump"
            }
        }
        val animations =
            objectMapOf<String, IAnimation>(
                "stand" pairTo Animation(atlas!!.findRegion("Robbit/Stand")),
                "crouch" pairTo Animation(atlas!!.findRegion("Robbit/Crouch")),
                "jump" pairTo Animation(atlas!!.findRegion("Robbit/Jump"))
            )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
