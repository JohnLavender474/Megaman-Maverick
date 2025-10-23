package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.Pendulum
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getMotionValue
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class SwingingAxe(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IBodyEntity, IMotionEntity,
    IDrawableShapesEntity {

    companion object {
        const val TAG = "SwingingAxe"
        private var textureRegion: TextureRegion? = null
        private const val DEBUG_SWING_ROTATION = false
        private const val LENGTH = 2.25f
        private const val PENDULUM_GRAVITY = 10f
        private const val DEBUG_SWING_ROTATION_SPEED = 1f
    }

    private val deathCircle = GameCircle()
    private val shieldCircle = GameCircle()
    private lateinit var pendulum: Pendulum

    private val debugSwingRotationTimer = Timer(DEBUG_SWING_ROTATION_SPEED)

    override fun init() {
        if (textureRegion == null) textureRegion = game.assMan.getTextureRegion(
            TextureAsset.HAZARDS_1.source, "SwingingAxe_HandleEndCentered"
        )
        addComponent(DrawableShapesComponent(debug = true))
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(MotionComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        clearMotionDefinitions()

        val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
        body.setCenter(bounds.getCenter())

        setPendulum(bounds)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM)

        val shapesComponent = getComponent(DrawableShapesComponent::class)!!

        deathCircle.setRadius(0.85f * ConstVals.PPM)
        val deathFixture = Fixture(body, FixtureType.DEATH, deathCircle)
        deathFixture.putProperty(ConstKeys.INSTANT, true)
        deathFixture.attachedToBody = false
        body.addFixture(deathFixture)
        shapesComponent.debugShapeSuppliers.add { deathCircle }

        shieldCircle.setRadius(0.85f * ConstVals.PPM)
        val shieldFixture = Fixture(body, FixtureType.SHIELD, shieldCircle)
        shieldFixture.attachedToBody = false
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)
        shapesComponent.debugShapeSuppliers.add { shieldCircle }

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(8f * ConstVals.PPM)
        sprite.setRegion(textureRegion!!)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { delta, _ ->
            val center = pendulum.anchor
            sprite.setCenter(center)
            sprite.setOriginCenter()
            sprite.setFlip(false, true)
            if (DEBUG_SWING_ROTATION) {
                debugSwingRotationTimer.update(delta)
                if (debugSwingRotationTimer.isFinished()) {
                    sprite.rotation -= 1f
                    debugSwingRotationTimer.reset()
                }
            } else sprite.rotation = MathUtils.radiansToDegrees * pendulum.angle * -1
        }
        return spritesComponent
    }

    private fun setPendulum(bounds: GameRectangle) {
        pendulum = Pendulum(
            LENGTH * ConstVals.PPM, PENDULUM_GRAVITY * ConstVals.PPM, bounds.getCenter(false), 1 / 60f
        )
        putMotionDefinition(
            ConstKeys.PENDULUM, MotionComponent.MotionDefinition(motion = pendulum, function = { value, _ ->
                deathCircle.setCenter(value)
                shieldCircle.setCenter(value)
            })
        )

        addDebugShapeSupplier {
            val line = GameLine(pendulum.anchor, pendulum.getMotionValue()!!)
            line.drawingColor = Color.DARK_GRAY
            line.drawingShapeType = ShapeRenderer.ShapeType.Line
            line
        }

        val circle1 = GameCircle()
        circle1.setRadius(ConstVals.PPM / 4f)
        circle1.drawingShapeType = ShapeRenderer.ShapeType.Filled
        circle1.drawingColor = Color.BROWN
        addDebugShapeSupplier { circle1.setCenter(pendulum.anchor) }

        val circle2 = GameCircle()
        circle2.setRadius(ConstVals.PPM / 4f)
        circle2.drawingShapeType = ShapeRenderer.ShapeType.Line
        circle2.drawingColor = Color.DARK_GRAY
        addDebugShapeSupplier { circle2.setCenter(pendulum.getMotionValue()!!) }
    }

    override fun getType() = EntityType.HAZARD
}
