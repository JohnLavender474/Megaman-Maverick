package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
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
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.hazards.DrippingToxicGoop
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class TankBot(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "TankBot"

        private const val X_VEL = 2f

        private const val TURN_DUR = 0.4f
        private const val TURN_DELAY = 0.25f

        private const val SHOOT_DELAY = 0.75f

        private const val LAUNCH_IMPULSE_X = 8f
        private const val LAUNCH_IMPULSE_Y = 8f
        private const val LAUNCH_GRAVITY = -0.15f

        private const val TOXIC_GOOP_DMG_DUR = 0.25f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    private val shootDelayTimer = Timer(SHOOT_DELAY)
    private val turnDelayTimer = Timer(TURN_DELAY)
    private val turnTimer = Timer(TURN_DUR)

    private var stopped = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            gdxArrayOf("roll", "stop", "turn").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
        damageOverrides.put(DrippingToxicGoop::class, dmgNeg(10))
    }

    override fun getDamageDuration(damager: IDamager) = when (damager) {
        is DrippingToxicGoop -> TOXIC_GOOP_DMG_DUR
        else -> super.getDamageDuration(damager)
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        shootDelayTimer.reset()

        turnDelayTimer.setToEnd()
        turnTimer.setToEnd()

        stopped = false
    }

    private fun shoot() {
        GameLogger.debug(TAG, "shoot()")

        val spawn = body.getCenter().add(0.3f * facing.value * ConstVals.PPM, 1.15f * ConstVals.PPM)

        val impulse = GameObjectPools.fetch(Vector2::class)
            .set(LAUNCH_IMPULSE_X * facing.value, LAUNCH_IMPULSE_Y)
            .scl(ConstVals.PPM.toFloat())

        val gravity = GameObjectPools.fetch(Vector2::class).set(0f, LAUNCH_GRAVITY * ConstVals.PPM)

        val bullet = MegaEntityFactory.fetch(Bullet::class)!!
        bullet.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.GRAVITY pairTo gravity,
            )
        )

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    private fun startTurning() {
        GameLogger.debug(TAG, "startTurning()")
        body.physics.velocity.x = 0f
        turnDelayTimer.reset()
    }

    override fun onHealthDepleted() {
        super.onHealthDepleted()

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.OWNER pairTo this))

        playSoundNow(SoundAsset.EXPLOSION_2_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!turnDelayTimer.isFinished()) {
                turnDelayTimer.update(delta)
                when {
                    turnDelayTimer.isFinished() -> turnTimer.reset()
                    else -> return@add
                }
            }

            if (!turnTimer.isFinished()) {
                turnTimer.update(delta)
                when {
                    turnTimer.isFinished() -> swapFacing()
                    else -> return@add
                }
            }

            if ((isFacing(Facing.LEFT) && megaman.body.getX() >= body.getMaxX()) ||
                (isFacing(Facing.RIGHT) && megaman.body.getMaxX() <= body.getX())
            ) {
                startTurning()
                return@add
            }

            if ((isFacing(Facing.LEFT) && !body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                (isFacing(Facing.RIGHT) && !body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))
            ) {
                stopped = true
                body.physics.velocity.x = 0f
            } else {
                stopped = false
                body.physics.velocity.x = X_VEL * ConstVals.PPM * facing.value
            }

            shootDelayTimer.update(delta)
            if (shootDelayTimer.isFinished()) {
                shoot()
                shootDelayTimer.reset()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(3f * ConstVals.PPM, 2f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.offsetFromBodyAttachment.set(-body.getWidth() / 2f, -body.getHeight() / 2f)
        leftFixture.offsetFromBodyAttachment.set(-body.getWidth() / 2f, -body.getHeight() / 2f)
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFixture.offsetFromBodyAttachment.set(body.getWidth() / 2f, -body.getHeight() / 2f)
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        val damagerFixture1 =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2f * ConstVals.PPM, ConstVals.PPM.toFloat()))
        damagerFixture1.attachedToBody = false
        body.addFixture(damagerFixture1)
        debugShapes.add { damagerFixture1 }

        val damageableFixture1 = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle())
        damageableFixture1.attachedToBody = false
        body.addFixture(damageableFixture1)

        val damagerFixture2 =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        damagerFixture2.attachedToBody = false
        body.addFixture(damagerFixture2)
        debugShapes.add { damagerFixture2 }

        val damageableFixture2 = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle())
        damageableFixture2.attachedToBody = false
        body.addFixture(damageableFixture2)

        val damagerFixture3 = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.5f * ConstVals.PPM))
        damagerFixture3.attachedToBody = false
        body.addFixture(damagerFixture3)
        debugShapes.add { damagerFixture3 }

        val damageableFixture3 = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle())
        damageableFixture3.attachedToBody = false
        body.addFixture(damageableFixture3)

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (turnTimer.isFinished()) {
                val position1 = if (isFacing(Facing.LEFT)) Position.BOTTOM_RIGHT else Position.BOTTOM_LEFT
                (damagerFixture1.rawShape as GameRectangle).positionOnPoint(body.getPositionPoint(position1), position1)
                (damageableFixture1.rawShape as GameRectangle).set(damagerFixture1.rawShape as GameRectangle)

                (damagerFixture2.rawShape as GameRectangle)
                    .setTopCenterToPoint(body.getPositionPoint(Position.TOP_CENTER))
                    .translate(0.25f * ConstVals.PPM * -facing.value, 0f)
                (damageableFixture2.rawShape as GameRectangle).set(damagerFixture2.rawShape as GameRectangle)

                damagerFixture3.setActive(true)
                (damagerFixture3.rawShape as GameRectangle).positionOnPoint(
                    (damagerFixture1.rawShape as GameRectangle).getPositionPoint(position1.flipHorizontally()),
                    position1
                )
                damageableFixture3.setActive(true)
                (damageableFixture3.rawShape as GameRectangle).set(damagerFixture3.rawShape as GameRectangle)
            } else {
                val position1 = Position.BOTTOM_CENTER
                (damagerFixture1.rawShape as GameRectangle).positionOnPoint(body.getPositionPoint(position1), position1)
                (damageableFixture1.rawShape as GameRectangle).set(damagerFixture1.rawShape as GameRectangle)

                (damagerFixture2.rawShape as GameRectangle).setTopCenterToPoint(body.getPositionPoint(Position.TOP_CENTER))
                (damageableFixture2.rawShape as GameRectangle).set(damagerFixture2.rawShape as GameRectangle)

                damagerFixture3.setActive(false)
                damageableFixture3.setActive(false)
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(3f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.hidden = damageBlink
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when {
                !turnTimer.isFinished() -> "turn"
                !turnDelayTimer.isFinished() || stopped -> "stop"
                else -> "roll"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "stop" pairTo Animation(regions["stop"]),
            "turn" pairTo Animation(regions["turn"], 2, 2, 0.1f, true),
            "roll" pairTo Animation(regions["roll"], 2, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
