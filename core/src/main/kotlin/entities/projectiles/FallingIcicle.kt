package com.megaman.maverick.game.entities.projectiles


import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.isSensing


class FallingIcicle(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "FallingIcicle"
        private const val SHAKE_DUR = 0.25f
        private const val GRAVITY = -0.15f
        private const val SHATTER_DUR = 0.25f
        private const val NORMAL_CLAMP_Y = 10f
        private const val WATER_CLAMP_Y = 3f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class FallingIcicleState { STILL, SHAKE, FALL }

    private val shakeTimer = Timer(SHAKE_DUR)
    private val shatterTimer = Timer(SHATTER_DUR)
    private lateinit var fallingIcicleState: FallingIcicleState

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

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getTopCenterPoint()
        body.setTopCenterToPoint(spawn)
        body.physics.gravityOn = false
        fallingIcicleState = FallingIcicleState.STILL
        shakeTimer.reset()
        shatterTimer.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "Falling icicle destroyed")
        super.onDestroy()
    }

    override fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "Explode and die")
        destroy()
        for (i in 0 until 5) {
            val iceShard = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.ICE_SHARD)!!
            iceShard.spawn(props(ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.INDEX pairTo i))
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when (fallingIcicleState) {
            FallingIcicleState.STILL -> {
                if (megaman.body.y < body.getMaxY() &&
                    megaman.body.getMaxX() > body.x &&
                    megaman.body.x < body.getMaxX()
                ) fallingIcicleState = FallingIcicleState.SHAKE
            }

            FallingIcicleState.SHAKE -> {
                shakeTimer.update(delta)
                if (shakeTimer.isFinished()) {
                    body.physics.gravityOn = true
                    fallingIcicleState = FallingIcicleState.FALL
                }
            }

            else -> {}
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.25f * ConstVals.PPM, 1.15f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val projectileFixture =
            Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.75f * ConstVals.PPM))
        projectileFixture.offsetFromBodyCenter.y = -0.125f * ConstVals.PPM
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val waterListener = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        body.addFixture(waterListener)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocityClamp.y =
                (if (body.isSensing(BodySense.IN_WATER)) WATER_CLAMP_Y else NORMAL_CLAMP_Y) * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getTopCenterPoint(), Position.TOP_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = {
            when (fallingIcicleState) {
                FallingIcicleState.STILL, FallingIcicleState.FALL -> "still"
                FallingIcicleState.SHAKE -> "shake"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "still" pairTo Animation(regions["still"]),
            "shake" pairTo Animation(regions["shake"], 2, 1, 0.1f, true),
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
