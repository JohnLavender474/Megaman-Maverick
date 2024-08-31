package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType


class FallingIcicle(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "FallingIcicle"
        private const val SHAKE_DUR = 0.25f
        private const val GRAVITY = -0.15f
        private const val SHATTER_DUR = 0.25f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class FallingIcicleState {
        STILL, SHAKE, FALL
    }

    private val shakeTimer = Timer(SHAKE_DUR)
    private val shatterTimer = Timer(SHATTER_DUR)
    private lateinit var state: FallingIcicleState

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_2.source)
            regions.put("still", atlas.findRegion("$TAG/still"))
            regions.put("shake", atlas.findRegion("$TAG/shake"))
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getTopCenterPoint()
        body.setTopCenterToPoint(spawn)

        body.physics.gravityOn = false

        state = FallingIcicleState.STILL

        shakeTimer.reset()
        shatterTimer.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "Falling icicle destroyed")
        super.onDestroy()
    }

    override fun hitBlock(blockFixture: IFixture) = explodeAndDie()

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "Explode and die")
        kill()
        for (i in 0 until 5) {
            val iceShard = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.ICE_SHARD)!!
            game.engine.spawn(iceShard, props(ConstKeys.POSITION to body.getCenter(), ConstKeys.INDEX to i))
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when (state) {
            FallingIcicleState.STILL -> {
                if (getMegaman().body.y < body.getMaxY() &&
                    getMegaman().body.getMaxX() > body.x &&
                    getMegaman().body.x < body.getMaxX()
                ) state = FallingIcicleState.SHAKE
            }

            FallingIcicleState.SHAKE -> {
                shakeTimer.update(delta)
                if (shakeTimer.isFinished()) {
                    body.physics.gravityOn = true
                    state = FallingIcicleState.FALL
                }
            }

            else -> {}
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.25f * ConstVals.PPM, ConstVals.PPM.toFloat())
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val projectileFixture =
            Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.75f * ConstVals.PPM))
        projectileFixture.offsetFromBodyCenter.y = -0.125f * ConstVals.PPM
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getTopCenterPoint(), Position.TOP_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = {
            when (state) {
                FallingIcicleState.STILL, FallingIcicleState.FALL -> "still"
                FallingIcicleState.SHAKE -> "shake"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "still" to Animation(regions["still"]),
            "shake" to Animation(regions["shake"], 2, 1, 0.1f, true),
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}