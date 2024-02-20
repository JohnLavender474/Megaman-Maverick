package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
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
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class ShieldAttacker(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        const val TAG = "ShieldAttacker"
        private var atlas: TextureAtlas? = null
        private const val TURN_AROUND_DUR = 0.5f
        private const val X_VEL = 6f
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10), Fireball::class to dmgNeg(ConstVals.MAX_HEALTH), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }, ChargedShotExplosion::class to dmgNeg(15)
    )

    override var facing = Facing.RIGHT

    private val turnAroundTimer = Timer(TURN_AROUND_DUR)

    private var minX = 0f
    private var maxX = 0f
    private var left = false

    private val turningAround: Boolean
        get() = !turnAroundTimer.isFinished()

    override fun init() {
        super.init()
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawn props: $spawnProps")
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val targetX = spawn.x + spawnProps.get(ConstKeys.VALUE, Float::class)!! * ConstVals.PPM
        if (spawn.x < targetX) {
            minX = spawn.x
            maxX = targetX
            left = false
        } else {
            minX = targetX
            maxX = spawn.x
            left = true
        }

        turnAroundTimer.reset()

        GameLogger.debug(TAG, "Start X: ${spawn.x}. Target X: $targetX. Left: $left")
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM, 1.5f * ConstVals.PPM)

        // damager fixture
        val damagerFixture = Fixture(GameRectangle(body), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)

        // damageable fixture
        val damageableFixture = Fixture(GameRectangle().setHeight(body.height), FixtureType.DAMAGEABLE)
        body.addFixture(damageableFixture)

        // shield fixture
        val shieldFixture =
            Fixture(
                GameRectangle().setSize(0.75f * ConstVals.PPM, 1.25f * ConstVals.PPM),
                FixtureType.SHIELD
            )
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)

        // pre-process
        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            val damageableShape = damageableFixture.shape as GameRectangle
            if (turningAround) {
                shieldFixture.active = false
                damageableFixture.offsetFromBodyCenter.x = 0f
                damageableShape.width = 0.5f * ConstVals.PPM
            } else {
                shieldFixture.active = true
                damageableFixture.offsetFromBodyCenter.x = (if (left) 0.5f else -0.5f) * ConstVals.PPM
                damageableShape.width = 0.15f * ConstVals.PPM
            }
        })

        return BodyComponentCreator.create(this, body)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            val centerX = body.getCenter().x
            if (centerX < minX || centerX > maxX) {
                turnAroundTimer.reset()
                body.setCenterX(if (centerX < minX) minX else maxX)
                body.physics.velocity.setZero()
                left = centerX >= maxX
            }

            turnAroundTimer.update(it)
            if (turnAroundTimer.isJustFinished()) {
                val x = X_VEL * ConstVals.PPM * (if (left) -1 else 1)
                body.physics.velocity.x = x
                GameLogger.debug(TAG, "Turning around. New x vel: $x")
            }
        }
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, "shieldattacker" to sprite)
        spritesComponent.putUpdateFunction("shieldattacker") { _, _sprite ->
            _sprite as GameSprite
            _sprite.setFlip(turningAround != left, false)
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (turningAround) "turn" else "attack" }
        val animations =
            objectMapOf<String, IAnimation>(
                "turn" to Animation(atlas!!.findRegion("ShieldAttacker/TurnAround"), 1, 5, 0.1f, false),
                "attack" to Animation(atlas!!.findRegion("ShieldAttacker/Attack"), 1, 2, 0.1f, true)
            )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
