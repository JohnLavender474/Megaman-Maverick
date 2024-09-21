package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
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

class ChargedShotExplosion(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        private const val FULLY_CHARGED_DURATION = 0.6f
        private const val HALF_CHARGED_DURATION = 0.3f
        private const val SOUND_INTERVAL = 0.15f

        private var fullyChargedRegion: TextureRegion? = null
        private var halfChargedRegion: TextureRegion? = null
    }

    var fullyCharged = false
        private set

    private var durationTimer = Timer(FULLY_CHARGED_DURATION)
    private val soundTimer = Timer(SOUND_INTERVAL)
    private lateinit var damagerFixture: Fixture
    private lateinit var direction: Direction

    override fun init() {
        if (fullyChargedRegion == null) fullyChargedRegion =
            game.assMan.getTextureRegion(TextureAsset.MEGAMAN_CHARGED_SHOT.source, "Collide")
        if (halfChargedRegion == null) halfChargedRegion =
            game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "HalfChargedShot")
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
        (damagerFixture.rawShape as GameRectangle).setSize(size)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val spriteDimension = (if (fullyCharged) 1.75f else 1.25f) * ConstVals.PPM
        (firstSprite as GameSprite).setSize(spriteDimension)
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

        damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBodyBounds() }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            val rotation = when (direction) {
                Direction.RIGHT -> 0f
                Direction.UP -> 90f
                Direction.LEFT -> 180f
                Direction.DOWN -> 270f
            }
            _sprite.rotation = rotation
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val chargedAnimation = Animation(fullyChargedRegion!!, 1, 3, .05f, true)
        val halfChargedAnimation = Animation(halfChargedRegion!!, 1, 3, .05f, true)
        val animator = Animator(
            { if (fullyCharged) "charged" else "halfCharged" },
            objectMapOf("charged" to chargedAnimation, "halfCharged" to halfChargedAnimation)
        )
        return AnimationsComponent(this, animator)
    }
}
