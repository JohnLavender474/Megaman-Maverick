package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.extensions.getTextureRegion
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class SnowballExplosion(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, ISpriteEntity, IDamager {

    companion object {
        const val DURATION = 0.075f
        private var region: TextureRegion? = null
    }

    private lateinit var damageMask: ObjectSet<KClass<out IDamageable>>
    private val timer = Timer(DURATION)

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "SnowballExplode")
        addComponent(defineAnimationsComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    @Suppress("UNCHECKED_CAST")
    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        damageMask =
            spawnProps.getOrDefault(ConstKeys.MASK, ObjectSet<KClass<out IDamageable>>())
                    as ObjectSet<KClass<out IDamageable>>
        timer.reset()
    }

    override fun canDamage(damageable: IDamageable) = damageMask.contains(damageable::class)

    override fun onDamageInflictedTo(damageable: IDamageable) {
        TODO("Not yet implemented")
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.5f * ConstVals.PPM)

        val spriteComponent = SpritesComponent(this, "snowballExplosion" to sprite)
        spriteComponent.putUpdateFunction("snowballExplosion") { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
        }

        return spriteComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 3, 0.025f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineUpdatablesComponent() =
        UpdatablesComponent(
            this,
            {
                timer.update(it)
                if (timer.isFinished()) kill(props(CAUSE_OF_DEATH_MESSAGE to "Timer finished"))
            })
}
