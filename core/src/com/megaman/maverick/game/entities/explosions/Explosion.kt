package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.audio.AudioComponent
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
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
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class Explosion(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, ISpriteEntity, IAudioEntity, IDamager {

    companion object {
        private var explosionRegion: TextureRegion? = null
        private const val DURATION = .275f
    }

    private val durationTimer = Timer(DURATION)
    private val damageMask = ObjectSet<Class<out IDamageable>>()

    override fun init() {
        if (explosionRegion == null) explosionRegion =
            game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "Explosion")

        addComponent(defineSpritesCompoent())
        addComponent(defineBodyComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(AudioComponent(this))
    }

    @Suppress("UNCHECKED_CAST")
    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION) as Vector2
        body.setCenter(spawn)

        durationTimer.reset()

        damageMask.clear()
        if (spawnProps.containsKey(ConstKeys.MASK)) {
            val _damageMask = spawnProps.get(ConstKeys.MASK) as ObjectSet<Class<out IDamageable>>
            damageMask.addAll(_damageMask)
        }

        if (spawnProps.getOrDefault(
                ConstKeys.SOUND,
                false,
                Boolean::class
            )
        ) requestToPlaySound(SoundAsset.EXPLOSION_SOUND, false)
    }

    override fun canDamage(damageable: IDamageable) = damageMask.contains(damageable.javaClass)

    override fun onDamageInflictedTo(damageable: IDamageable) {
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, {
        durationTimer.update(it)
        if (durationTimer.isFinished()) kill(props(CAUSE_OF_DEATH_MESSAGE to "Duration timer finished"))
    })

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(explosionRegion!!, 1, 11, .025f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
        sprite.setSize(2.5f * ConstVals.PPM)

        val SpritesComponent = SpritesComponent(this, "explosion" to sprite)
        SpritesComponent.putUpdateFunction("explosion") { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
        }

        return SpritesComponent
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())

        // damager fixture
        val damagerFixture = Fixture(GameRectangle(body), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)

        return BodyComponentCreator.create(this, body)
    }
}
