package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.audio.AudioComponent
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpriteEntity
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
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Explosion(game: MegamanMaverickGame) : GameEntity(game), IHazard, IOwnable, IBodyEntity, ISpriteEntity,
    IAudioEntity, IDamager {

    companion object {
        private var explosionRegion: TextureRegion? = null
        private const val DURATION = .275f
    }

    override var owner: IGameEntity? = null

    private val durationTimer = Timer(DURATION)

    override fun init() {
        if (explosionRegion == null) explosionRegion =
            game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "Explosion")

        addComponent(defineSpritesCompoent())
        addComponent(defineBodyComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(AudioComponent(this))
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION) as Vector2
        body.setCenter(spawn)
        durationTimer.reset()
        owner = spawnProps.get(ConstKeys.OWNER) as IGameEntity?
        if (spawnProps.containsKey(ConstKeys.SOUND)) {
            val sound = spawnProps.get(ConstKeys.SOUND, SoundAsset::class)!!
            requestToPlaySound(sound, false)
        }
    }

    override fun canDamage(damageable: IDamageable) = damageable != owner

    override fun onDamageInflictedTo(damageable: IDamageable) {}

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, {
        durationTimer.update(it)
        if (durationTimer.isFinished()) kill()
    })

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(explosionRegion!!, 1, 11, .025f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.5f * ConstVals.PPM)
        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)
        return BodyComponentCreator.create(this, body)
    }
}
