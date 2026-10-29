package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.IAudioEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter

class AsteroidExplosion(game: MegaServices) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IAudioEntity, IDamagerEntity, IOwnable<IGameEntity> {

    companion object {
        const val TAG = "AsteroidExplosion"
        private const val EXPLOSION_DUR = 0.5f
        private var region: TextureRegion? = null
    }

    override var owner: IGameEntity? = null

    private var damager = true
    private val timer = Timer(EXPLOSION_DUR)

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, TAG)
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER) as GameEntity?

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        timer.reset()

        val sound = spawnProps.getOrDefault(ConstKeys.SOUND, true, Boolean::class)
        if (sound &&
            overlapsGameCamera() &&
            !game.audioMan.isSoundPlaying(SoundAsset.ASTEROID_EXPLODE_SOUND)
        ) requestToPlaySound(SoundAsset.ASTEROID_EXPLODE_SOUND, false)

        damager = spawnProps.getOrDefault(ConstKeys.DAMAGER, false, Boolean::class)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        timer.update(delta)
        if (timer.isFinished()) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()

        val explosionFixture = Fixture(body, FixtureType.EXPLOSION, GameCircle().setRadius(1f * ConstVals.PPM))
        body.addFixture(explosionFixture)
        explosionFixture.drawingColor = Color.RED
        debugShapes.add { explosionFixture}

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(1f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.fixtures[FixtureType.DAMAGER].first().setActive(damager)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ -> sprite.setCenter(body.getCenter()) }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 3, 2, 0.1f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.EXPLOSION

    override fun getTag() = TAG
}
