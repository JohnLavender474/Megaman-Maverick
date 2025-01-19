package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.SineWave
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.Asteroid
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getMotionValue
import com.megaman.maverick.game.world.body.*

class TellySaucer(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IMotionEntity {

    companion object {
        const val TAG = "TellySaucer"

        private const val SPEED = 4f
        private const val FREQUENCY = 3f
        private const val AMPLITUDE = 0.025f

        private const val HIT_DUR = 1f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private lateinit var sine: SineWave

    private val hitTimer = Timer(HIT_DUR)
    private val hit: Boolean
        get() = !hitTimer.isFinished()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        
        damageOverrides.put(Asteroid::class, dmgNeg(ConstVals.MAX_HEALTH))

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            gdxArrayOf("spin", "hit").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }

        super.init()

        addComponent(MotionComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val left = spawnProps.getOrDefault(ConstKeys.LEFT, megaman.body.getX() <= body.getX(), Boolean::class)
        val flip = spawnProps.getOrDefault(ConstKeys.FLIP, false, Boolean::class)

        val sinePos = body.getCenter(false)
        val speed = ConstVals.PPM * if (left) -SPEED else SPEED
        val amplitude = ConstVals.PPM * if (flip) -AMPLITUDE else AMPLITUDE
        sine = SineWave(sinePos, speed, amplitude, FREQUENCY)

        hitTimer.setToEnd()

        requestToPlaySound(SoundAsset.ALARM_SOUND, false)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super.onDestroy()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        GameLogger.debug(TAG, "onDamageInflictedTo(): damageable=$damageable")

        hitTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            hitTimer.update(delta)

            if (hitTimer.isFinished()) {
                sine.update(delta)

                val center = sine.getMotionValue()
                center?.let { body.setCenter(it) }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.hidden = damageBlink

            sprite.setCenter(body.getCenter())

            sprite.setOriginCenter()
            sprite.rotation = megaman.direction.rotation
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (hit) "hit" else "spin" }
                .addAnimations(
                    "hit" pairTo Animation(regions["hit"], 2, 1, 0.1f, true),
                    "spin" pairTo Animation(regions["spin"], 3, 2, 0.1f, true)
                )
                .build()
        )
        .build()
}
