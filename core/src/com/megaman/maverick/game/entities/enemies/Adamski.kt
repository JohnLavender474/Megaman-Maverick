package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IMotionEntity
import com.engine.motion.MotionComponent
import com.engine.motion.MotionComponent.MotionDefinition
import com.engine.motion.SineWave
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
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

class Adamski(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IMotionEntity {

    companion object {
        const val TAG = "Adamski"
        private const val SPEED = 4f
        private const val FREQUENCY = 3f
        private const val AMPLITUDE = 0.025f
        private var purpleRegion: TextureRegion? = null
        private var blueRegion: TextureRegion? = null
        private var orangeRegion: TextureRegion? = null
    }

    override val damageNegotiations =
        objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class to dmgNeg(10),
            Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
            ChargedShot::class to dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
            },
            ChargedShotExplosion::class to dmgNeg(ConstVals.MAX_HEALTH)
        )

    private var type = 0

    override fun init() {
        if (purpleRegion == null || blueRegion == null || orangeRegion == null) {
            purpleRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Adamski/Purple")
            blueRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Adamski/Blue")
            orangeRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Adamski/Orange")
        }
        super<AbstractEnemy>.init()
        addComponent(MotionComponent(this))
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawning Adamski with props = $spawnProps")
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, 0, Int::class)

        val left = spawnProps.getOrDefault(ConstKeys.LEFT, megaman.body.x <= body.x, Boolean::class)
        val flip = spawnProps.getOrDefault(ConstKeys.FLIP, false, Boolean::class)
        val motion = SineWave(
            body.getCenter(),
            (if (left) -SPEED else SPEED) * ConstVals.PPM,
            AMPLITUDE * ConstVals.PPM * if (flip) -1f else 1f,
            FREQUENCY
        )
        putMotionDefinition("sineWave", MotionDefinition(motion, { position, _ ->
            body.setCenter(position)
        }))

        requestToPlaySound(SoundAsset.ALARM_SOUND, false)
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        GameLogger.debug(TAG, "Adamski destroyed at position = ${body.getCenter()}")
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.65f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)
        bodyFixture.getShape().color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)
        damageableFixture.getShape().color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { "$type" }
        val animations = objectMapOf<String, IAnimation>(
            "0" to Animation(purpleRegion!!, 1, 2, 0.1f, true),
            "1" to Animation(blueRegion!!, 1, 2, 0.1f, true),
            "2" to Animation(orangeRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

}