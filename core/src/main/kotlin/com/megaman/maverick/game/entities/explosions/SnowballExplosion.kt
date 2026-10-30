package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter
import kotlin.reflect.KClass

class SnowballExplosion(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, IHazard,
    IDamagerEntity {

    companion object {
        const val TAG = "SnowballExplosion"
        const val DURATION = 0.015f
        private var region: TextureRegion? = null
    }

    private lateinit var damageMask: ObjectSet<KClass<out IDamageableEntity>>
    private val timer = Timer(DURATION)

    override fun getType() = EntityType.EXPLOSION

    override fun init(vararg params: Any) {
        if (region == null) region = services.assets.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, "SnowballExplode")
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        damageMask = spawnProps.getOrDefault(
            ConstKeys.MASK,
            ObjectSet<KClass<out IDamageableEntity>>()
        ) as ObjectSet<KClass<out IDamageableEntity>>

        timer.reset()
    }

    override fun canDamage(damageable: IDamageableEntity) = damageMask.contains(damageable::class)

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.5f * ConstVals.PPM)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ damagerFixture }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.5f * ConstVals.PPM)
        val spriteComponent = SpritesComponent(sprite)
        spriteComponent.putPreProcess { _, _ -> sprite.setCenter(body.getCenter()) }
        return spriteComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 3, 0.005f, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        timer.update(it)
        if (timer.isFinished()) destroy()
    })
}
