package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
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
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class ChargedShotExplosion(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "ChargedShotExplosion"

        private const val FULLY_CHARGED_DURATION = 0.6f
        private const val HALF_CHARGED_DURATION = 0.3f
        private const val SOUND_INTERVAL = 0.15f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    var fullyCharged = false
        private set

    private var durationTimer = Timer(FULLY_CHARGED_DURATION)
    private val soundTimer = Timer(SOUND_INTERVAL)

    private lateinit var explosionFixture: Fixture
    private lateinit var damagerFixture: Fixture

    private lateinit var direction: Direction

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.EXPLOSIONS_1.source)
            regions.put("full", atlas.findRegion("ChargedShotExplosion"))
            regions.put("half", atlas.findRegion("HalfChargedShotExplosion"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        soundTimer.reset()

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        fullyCharged = spawnProps.get(ConstKeys.BOOLEAN, Boolean::class)!!

        val duration = spawnProps.getOrDefault(
            ConstKeys.DURATION, if (fullyCharged) FULLY_CHARGED_DURATION else HALF_CHARGED_DURATION, Float::class
        )
        durationTimer = Timer(duration)

        val size = if (fullyCharged) 1.5f * ConstVals.PPM else ConstVals.PPM.toFloat()
        body.setSize(size)

        (explosionFixture.rawShape as GameRectangle).setSize(size)
        (damagerFixture.rawShape as GameRectangle).setSize(size)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val spriteDimension = (if (fullyCharged) 1.75f else 1.25f) * ConstVals.PPM
        defaultSprite.setSize(spriteDimension)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        durationTimer.update(it)
        if (durationTimer.isFinished()) destroy()

        soundTimer.update(it)
        if (soundTimer.isFinished() && overlapsGameCamera()) {
            requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
            soundTimer.reset()
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        explosionFixture = Fixture(body, FixtureType.EXPLOSION, GameRectangle())
        body.addFixture(explosionFixture)

        damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            val rotation = when (direction) {
                Direction.RIGHT -> 0f
                Direction.UP -> 90f
                Direction.LEFT -> 180f
                Direction.DOWN -> 270f
            }
            sprite.rotation = rotation
            sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (fullyCharged) "full" else "half" }
        val animations = objectMapOf<String, IAnimation>(
            "full" pairTo Animation(regions["full"], 3, 1, 0.05f, true),
            "half" pairTo Animation(regions["half"], 3, 1, 0.05f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
