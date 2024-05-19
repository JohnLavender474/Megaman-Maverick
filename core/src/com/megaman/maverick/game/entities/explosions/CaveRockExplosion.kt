package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
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
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpritesEntity
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

class CaveRockExplosion(game: MegamanMaverickGame) :
    GameEntity(game), IDamager, IBodyEntity, IAudioEntity, ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "CaveRockExplosion"
        private var burstRegion: TextureRegion? = null
        private const val DURATION = 0.15f
    }

    private val timer = Timer(DURATION)

    override fun init() {
        if (burstRegion == null)
            burstRegion =
                game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "CaveRock/Burst")
        addComponent(AudioComponent(this))
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        requestToPlaySound(SoundAsset.BURST_SOUND, false)
        timer.reset()
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(1f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineUpdatablesComponent() =
        UpdatablesComponent(
            this,
            {
                timer.update(it)
                if (timer.isFinished()) kill(props(CAUSE_OF_DEATH_MESSAGE to "Duration ended"))
            })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(burstRegion!!, 1, 3, 0.05f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }
}
