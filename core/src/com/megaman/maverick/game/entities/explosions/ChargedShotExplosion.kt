package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class ChargedShotExplosion(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        private const val FULLY_CHARGED_DURATION = .6f
        private const val HALF_CHARGED_DURATION = .3f
        private const val SOUND_INTERVAL = .15f

        private var fullyChargedRegion: TextureRegion? = null
        private var halfChargedRegion: TextureRegion? = null
    }

    override var owner: IGameEntity? = null
    var fullyCharged = false
        private set

    private var durationTimer = Timer(FULLY_CHARGED_DURATION)
    private val soundTimer = Timer(SOUND_INTERVAL)
    private lateinit var direction: Direction

    override fun init() {
        if (fullyChargedRegion == null)
            fullyChargedRegion =
                game.assMan.getTextureRegion(TextureAsset.MEGAMAN_CHARGED_SHOT.source, "Collide")

        if (halfChargedRegion == null)
            halfChargedRegion =
                game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "HalfChargedShot")

        addComponents(defineProjectileComponents())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        soundTimer.reset()

        owner = spawnProps.get(ConstKeys.OWNER) as IGameEntity
        direction = spawnProps.get(ConstKeys.DIRECTION, Direction::class)!!
        fullyCharged = spawnProps.get(ConstKeys.BOOLEAN) as Boolean

        durationTimer = Timer(if (fullyCharged) FULLY_CHARGED_DURATION else HALF_CHARGED_DURATION)

        val spawn = spawnProps.get(ConstKeys.POSITION) as Vector2
        body.setCenter(spawn)

        val spriteDimension = (if (fullyCharged) 1.75f else 1.25f) * ConstVals.PPM
        (firstSprite as GameSprite).setSize(spriteDimension)
    }

    private fun defineUpdatablesComponent() =
        UpdatablesComponent(
            this,
            {
                durationTimer.update(it)
                if (durationTimer.isFinished()) {
                    kill(props(CAUSE_OF_DEATH_MESSAGE to "Duration timer finished"))
                }

                soundTimer.update(it)
                if (soundTimer.isFinished()) {
                    requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
                    soundTimer.reset()
                }
            })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())

        // damager fixture
        val damagerFixture =
            Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat()), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        val spritesComponent = SpritesComponent(this, "explosion" to sprite)
        spritesComponent.putUpdateFunction("explosion") { _, _sprite ->
            (_sprite as GameSprite).setPosition(body.getCenter(), Position.CENTER)

            val rotation =
                when (direction) {
                    Direction.RIGHT -> 0f
                    Direction.UP -> 90f
                    Direction.LEFT -> 180f
                    Direction.DOWN -> 270f
                }
            _sprite.setOriginCenter()
            _sprite.rotation = rotation
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val chargedAnimation = Animation(fullyChargedRegion!!, 1, 3, .05f, true)
        val halfChargedAnimation = Animation(halfChargedRegion!!, 1, 3, .05f, true)
        val animator =
            Animator(
                { if (fullyCharged) "charged" else "halfCharged" },
                objectMapOf("charged" to chargedAnimation, "halfCharged" to halfChargedAnimation)
            )
        return AnimationsComponent(this, animator)
    }
}
