package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.entities.IGameEntity
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
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter

class Explosion(game: MegaServices) : MegaGameEntity(game), IHazard, IOwnable<IGameEntity>, IBodyEntity,
    ISpritesEntity, IAudioEntity, IDamagerEntity {

    companion object {
        const val TAG = "Explosion"
        private var region: TextureRegion? = null
        private const val DURATION = 0.275f
    }

    override var owner: IGameEntity? = null

    private val durationTimer = Timer(DURATION)
    private var damager = true

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, TAG)
        super.init()
        addComponent(defineSpritesCompoent())
        addComponent(defineBodyComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        durationTimer.reset()

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        damager = spawnProps.getOrDefault(ConstKeys.DAMAGER, true, Boolean::class)

        if (spawnProps.containsKey(ConstKeys.SOUND) && overlapsGameCamera()) {
            val sound = spawnProps.get(ConstKeys.SOUND, SoundAsset::class)!!
            requestToPlaySound(sound, false)
        }
    }

    override fun canDamage(damageable: IDamageableEntity) = damager && damageable != owner

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        durationTimer.update(it)
        if (durationTimer.isFinished()) destroy()
    })

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 11, .025f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 10))
        sprite.setSize(2.5f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ -> sprite.setCenter(body.getCenter()) }
        return component
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(ConstVals.PPM.toFloat()))
        body.addFixture(damagerFixture)

        val explosionFixture = Fixture(body, FixtureType.EXPLOSION, GameCircle().setRadius(ConstVals.PPM.toFloat()))
        body.addFixture(explosionFixture)

        addComponent(
            DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ damagerFixture }), debug = true)
        )

        return BodyComponentCreator.create(this, body)
    }

    override fun getType() = EntityType.EXPLOSION
}
