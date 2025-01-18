package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.MotionComponent.MotionDefinition
import com.mega.game.engine.motion.SineWave
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter

class Adamski(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IMotionEntity {

    companion object {
        const val TAG = "Adamski"

        private const val SPEED = 4f
        private const val FREQUENCY = 3f
        private const val AMPLITUDE = 0.025f

        private var purpleRegion: TextureRegion? = null
        private var blueRegion: TextureRegion? = null
        private var orangeRegion: TextureRegion? = null
    }

    private var type = 0

    override fun init() {
        if (purpleRegion == null || blueRegion == null || orangeRegion == null) {
            purpleRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "$TAG/Purple")
            blueRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "$TAG/Blue")
            orangeRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "$TAG/Orange")
        }
        super.init()
        addComponent(MotionComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawning Adamski with props = $spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, 0, Int::class)

        val left = spawnProps.getOrDefault(ConstKeys.LEFT, megaman.body.getX() <= body.getX(), Boolean::class)
        val flip = spawnProps.getOrDefault(ConstKeys.FLIP, false, Boolean::class)
        val motion = SineWave(
            body.getCenter(false),
            (if (left) -SPEED else SPEED) * ConstVals.PPM,
            AMPLITUDE * ConstVals.PPM * if (flip) -1f else 1f,
            FREQUENCY
        )
        putMotionDefinition("sineWave", MotionDefinition(motion, { position, _ -> body.setCenter(position) }))

        requestToPlaySound(SoundAsset.ALARM_SOUND, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        GameLogger.debug(TAG, "Adamski destroyed at position = ${body.getCenter()}")
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink

            sprite.setCenter(body.getCenter())

            sprite.setOriginCenter()
            sprite.rotation = megaman.direction.rotation
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { "$type" }
        val animations = objectMapOf<String, IAnimation>(
            "0" pairTo Animation(purpleRegion!!, 1, 2, 0.1f, true),
            "1" pairTo Animation(blueRegion!!, 1, 2, 0.1f, true),
            "2" pairTo Animation(orangeRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

}
