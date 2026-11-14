package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.setToDirection
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
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

class BigAssMaverickRobotOrbExplosion(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity,
    IAnimatedEntity, IDamagerEntity, IHazard, IDirectional {

    companion object {
        const val TAG = "BigAssMaverickRobotOrbExplosion"
        private const val SHOCK_DUR = 0.5f
        private const val DISSIPATE_DUR = 0.3f
        private const val PULSE_SIZE = 1f
        private const val DISSIPATE_SIZE = 0.5f
        private var explosionRegion: TextureRegion? = null
        private var dissipateRegion: TextureRegion? = null
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    private val outward = Vector2()
    private val shockTimer = Timer(SHOCK_DUR)
    private val dissipateTimer = Timer(DISSIPATE_DUR)

    override fun getType() = EntityType.EXPLOSION

    override fun init(vararg params: Any) {
        super.init()
        GameLogger.debug(TAG, "init()")
        if (explosionRegion == null || dissipateRegion == null) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.EXPLOSIONS_1.source)
            explosionRegion = atlas.findRegion("$TAG/ElectricPulse")
            dissipateRegion = atlas.findRegion("$TAG/ElectricDissipate")
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)
        val base = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val center = outward.setToDirection(direction).scl(body.getHeight() / 2f).add(base)
        body.setCenter(center.x, center.y)
        shockTimer.reset()
        dissipateTimer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        shockTimer.update(delta)
        if (shockTimer.isFinished()) {
            dissipateTimer.update(delta)
            if (dissipateTimer.isFinished()) destroy()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.65f * ConstVals.PPM, 1.05f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(
                0.5f * ConstVals.PPM,
                ConstVals.PPM.toFloat()
            )
        )
        body.addFixture(damagerFixture)
        damagerFixture.drawingColor = Color.RED
        debugShapes.add { damagerFixture }

        body.preProcess.put(ConstKeys.DEFAULT) { damagerFixture.setActive(!shockTimer.isFinished()) }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            val size = (if (shockTimer.isFinished()) DISSIPATE_SIZE else PULSE_SIZE) * ConstVals.PPM
            sprite.setSize(size)
            outward.setToDirection(direction)
            val center = body.getCenter()
            sprite.setCenter(
                center.x + outward.x * (size - body.getHeight()) / 2f,
                center.y + outward.y * (size - body.getHeight()) / 2f
            )
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySuppplier: (String?) -> String? = { if (shockTimer.isFinished()) "dissipate" else "pulse" }
        val animations = objectMapOf<String, IAnimation>(
            "dissipate" pairTo Animation(dissipateRegion!!, 1, 3, 0.1f, false),
            "pulse" pairTo Animation(explosionRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySuppplier, animations)
        return AnimationsComponent(this, animator)
    }
}
