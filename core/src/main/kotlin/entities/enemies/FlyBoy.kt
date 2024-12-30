package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
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
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class FlyBoy(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "FlyBoy"
        const val STAND_DUR = 0.75f
        const val FLY_DUR = 2f
        const val FLY_VEL = 6f
        const val GRAV = -0.2f
        const val G_GRAV = -0.015f
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(5),
        Fireball::class pairTo dmgNeg(10),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )
    override var facing = Facing.RIGHT

    val flying: Boolean
        get() = !flyTimer.isFinished()
    val standing: Boolean
        get() = !standTimer.isFinished()

    private val flyTimer = Timer(FLY_DUR)
    private val standTimer = Timer(STAND_DUR)

    override fun getTag() = TAG

    override fun init() {
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        standTimer.reset()
        flyTimer.setToEnd()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (hasDepletedHealth()) explode()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), ConstVals.PPM * 2f)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture}

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM * 0.5f))
        feetFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ ->
            if (overlapsGameCamera()) requestToPlaySound(SoundAsset.MARIO_FIREBALL_SOUND, false)
        }
        feetFixture.offsetFromBodyAttachment.y = -ConstVals.PPM.toFloat()
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture}

        val headFixture = Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM * 0.5f))
        headFixture.offsetFromBodyAttachment.y = ConstVals.PPM.toFloat()
        body.addFixture(headFixture)
        debugShapes.add { headFixture}

        val damagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(0.8f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        )
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture}

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture}

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravityOn = standing
            body.physics.gravity.y = (if (body.isSensing(BodySense.FEET_ON_GROUND)) G_GRAV else GRAV) * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            _sprite.setFlip(facing == Facing.LEFT, false)
        }
        return spritesComponent
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            facing = if (body.getX() > megaman.body.getX()) Facing.LEFT else Facing.RIGHT
            if (body.isSensing(BodySense.FEET_ON_GROUND)) body.physics.velocity.x = 0f
            if (standing && body.isSensing(BodySense.FEET_ON_GROUND)) {
                standTimer.update(it)
                if (standTimer.isJustFinished()) {
                    flyTimer.reset()
                    body.physics.velocity.y = FLY_VEL * ConstVals.PPM
                }
            }
            if (flying) {
                flyTimer.update(it)
                if (flyTimer.isJustFinished() || body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) {
                    flyTimer.setToEnd()
                    standTimer.reset()
                    impulseToPlayer()
                }
            }
        }
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        val animations = objectMapOf<String, IAnimation>(
            "fly" pairTo Animation(atlas.findRegion("FlyBoy/Fly"), 1, 4, 0.1f),
            "stand" pairTo Animation(atlas.findRegion("FlyBoy/Stand"))
        )
        val keySupplier = { if (flying) "fly" else "stand" }
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun impulseToPlayer() {
        body.physics.velocity.x = 1.85f * (megaman.body.getX() - body.getX())
        body.physics.velocity.y = 0f
    }
}
