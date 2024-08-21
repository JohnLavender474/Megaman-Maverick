package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IMotionEntity
import com.engine.motion.MotionComponent
import com.engine.motion.SineWave
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class CarriCarry(game: MegamanMaverickGame) : AbstractEnemy(game), IMotionEntity, IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "CarriCarry"
        private const val SINE_SPEED = 3f
        private const val SINE_AMPLITUDE = 3f
        private const val SINE_FREQUENCY = 3f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    override lateinit var facing: Facing

    private var shake = false
    private var centerX = 0f

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("ride", atlas.findRegion("$TAG/ride"))
            regions.put("shake", atlas.findRegion("$TAG/shake"))
        }
        super<AbstractEnemy>.init()
        addComponent(MotionComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawn(): spawn props = $spawnProps")
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        shake = false

        centerX = body.getCenter().x
        val sine = SineWave(Vector2(0f, 1f), SINE_SPEED, SINE_AMPLITUDE, SINE_FREQUENCY)
        putMotionDefinition(ConstKeys.MOVE, MotionComponent.MotionDefinition(sine, { value, _ ->
            body.setCenterX(centerX + value.y)
            GameLogger.debug(TAG, "Set to center x: ${body.getCenter().x}")
        }))
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super<AbstractEnemy>.onDestroy()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.25f * ConstVals.PPM, ConstVals.PPM.toFloat())
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(
                ConstVals.PPM.toFloat(), 0.5f * ConstVals.PPM
            )
        )
        damageableFixture.offsetFromBodyCenter.y = 0.35f * ConstVals.PPM
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.475f * ConstVals.PPM, 1.875f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (shake) "shake" else "ride" }
        val animations = objectMapOf<String, IAnimation>(
            "shake" to Animation(regions.get("shake"), 2, 1, 0.1f, true),
            "ride" to Animation(regions.get("ride"), 2, 1, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}