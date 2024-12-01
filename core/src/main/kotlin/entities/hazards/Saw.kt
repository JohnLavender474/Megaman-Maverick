package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.*
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.IMotionEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.motion.MotionComponent
import com.mega.game.engine.motion.MotionComponent.MotionDefinition
import com.mega.game.engine.motion.Pendulum
import com.mega.game.engine.motion.RotatingLine
import com.mega.game.engine.motion.Trajectory
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
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter

class Saw(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IMotionEntity,
    ICullableEntity, IFaceable {

    companion object {
        const val TAG = "Saw"
        const val PENDULUM_TYPE = "p"
        const val ROTATION_TYPE = "r"
        const val TRAJECTORY_TYPE = "t"
        private var sawRegion: TextureRegion? = null
        private var ringRegion: TextureRegion? = null
        private const val LENGTH = 3f
        private const val ROTATION_SPEED = 2f
        private const val PENDULUM_GRAVITY = 10f
        private const val RING_COUNT = 6
    }

    override lateinit var facing: Facing

    override fun getTag() = TAG

    override fun getEntityType() = EntityType.HAZARD

    override fun init() {
        if (sawRegion == null || ringRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            sawRegion = atlas.findRegion("$TAG/saw")
            ringRegion = atlas.findRegion("$TAG/ring")
        }
        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(MotionComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        clearMotionDefinitions()
        val bounds = spawnProps.get(ConstKeys.BOUNDS) as GameRectangle
        val type = spawnProps.get(ConstKeys.TYPE) as String
        when (type.lowercase()) {
            PENDULUM_TYPE -> setToPendulum(bounds, spawnProps)
            ROTATION_TYPE -> setToRotation(bounds, spawnProps)
            TRAJECTORY_TYPE -> {
                val trajectory = spawnProps.get(ConstKeys.TRAJECTORY) as String
                setToTrajectory(bounds, trajectory)
            }
        }
        facing = Facing.valueOf(spawnProps.getOrDefault(ConstKeys.FACING, "left", String::class).uppercase())
    }

    private fun setToPendulum(bounds: GameRectangle, spawnProps: Properties) {
        val length = spawnProps.getOrDefault(ConstKeys.LENGTH, LENGTH, Float::class)
        val gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, PENDULUM_GRAVITY, Float::class)
        val pendulum = Pendulum(length * ConstVals.PPM, gravity * ConstVals.PPM, bounds.getCenter(), 1 / 60f)
        putMotionDefinition(
            ConstKeys.PENDULUM, MotionDefinition(motion = pendulum, function = { value, _ ->
                body.setCenter(value)
            })
        )

        for (i in 0..RING_COUNT) putUpdateFunction("ring_$i") { _, sprite ->
            val distance = (i.toFloat() / RING_COUNT.toFloat()) * pendulum.length
            val center = pendulum.getPointFromAnchor(distance)
            sprite.setCenter(center)
            sprite.hidden = false
        }

        val debugShapes = Array<() -> IDrawableShape?>()

        debugShapes.add {
            val line = GameLine(pendulum.anchor, body.getCenter())
            line.drawingColor = Color.DARK_GRAY
            line.drawingShapeType = ShapeRenderer.ShapeType.Filled
            line
        }

        val circle1 = GameCircle()
        circle1.setRadius(ConstVals.PPM / 8f)
        circle1.drawingShapeType = ShapeRenderer.ShapeType.Filled
        circle1.drawingColor = Color.DARK_GRAY
        debugShapes.add { circle1.setCenter(pendulum.anchor) }

        val circle2 = GameCircle()
        circle2.setRadius(ConstVals.PPM / 4f)
        circle2.drawingShapeType = ShapeRenderer.ShapeType.Filled
        circle2.drawingColor = Color.DARK_GRAY
        debugShapes.add { circle2.setCenter(body.getCenter()) }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))
    }

    private fun setToRotation(bounds: GameRectangle, spawnProps: Properties) {
        val length = spawnProps.getOrDefault(ConstKeys.LENGTH, LENGTH, Float::class)
        val startRotation = spawnProps.getOrDefault(ConstKeys.ROTATION, 0f, Float::class)
        val speed = spawnProps.getOrDefault(ConstKeys.SPEED, ROTATION_SPEED, Float::class)
        val rotation = RotatingLine(bounds.getCenter(), length * ConstVals.PPM, speed * ConstVals.PPM, startRotation)
        putMotionDefinition(
            ConstKeys.ROTATION, MotionDefinition(motion = rotation, function = { value, _ -> body.setCenter(value) })
        )

        for (i in 0..RING_COUNT) putUpdateFunction("ring_$i") { _, sprite ->
            val scale = i.toFloat() / RING_COUNT.toFloat()
            val center = rotation.getScaledPosition(scale, GameObjectPools.fetch(Vector2::class))
            sprite.setCenter(center)
            sprite.hidden = false
        }

        val debugShapes = Array<() -> IDrawableShape?>()

        debugShapes.add {
            val line = GameLine(rotation.getOrigin(), body.getCenter())
            line.drawingColor = Color.DARK_GRAY
            line.drawingShapeType = ShapeRenderer.ShapeType.Filled
            line
        }

        val circle1 = GameCircle()
        circle1.setRadius(ConstVals.PPM / 8f)
        circle1.drawingColor = Color.DARK_GRAY
        circle1.drawingShapeType = ShapeRenderer.ShapeType.Filled
        debugShapes.add { circle1.setCenter(rotation.getOrigin()) }

        val circle2 = GameCircle()
        circle2.setRadius(ConstVals.PPM / 4f)
        circle2.drawingColor = Color.DARK_GRAY
        circle2.drawingShapeType = ShapeRenderer.ShapeType.Filled
        debugShapes.add { circle2.setCenter(body.getCenter()) }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))
    }

    private fun setToTrajectory(bounds: GameRectangle, trajectoryDefinition: String) {
        val spawn = bounds.getCenter()
        body.setCenter(spawn)

        val trajectory = Trajectory(trajectoryDefinition, ConstVals.PPM)
        putMotionDefinition(
            ConstKeys.TRAJECTORY, MotionDefinition(motion = trajectory,
                function = { value, _ -> body.setCenter(value) },
                onReset = { body.setCenter(spawn) })
        )

        for (i in 0..RING_COUNT) putUpdateFunction("ring_$i") { _, sprite -> sprite.hidden = true }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM)

        val deathFixture = Fixture(body, FixtureType.DEATH, GameCircle().setRadius(0.85f * ConstVals.PPM))
        deathFixture.putProperty(ConstKeys.INSTANT, true)
        body.addFixture(deathFixture)

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameCircle().setRadius(0.85f * ConstVals.PPM))
        shieldFixture.putProperty(ConstKeys.DIRECTION, Direction.UP)
        body.addFixture(shieldFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val spritesComponent = SpritesComponent()

        val sawSprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sawSprite.setSize(2f * ConstVals.PPM)
        spritesComponent.sprites.put("saw", sawSprite)
        spritesComponent.putUpdateFunction("saw") { _, sprite ->
            sprite.setPosition(body.getCenter(), Position.CENTER)
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }

        for (i in 0..RING_COUNT) {
            val ringSprite = GameSprite(ringRegion!!, DrawingPriority(DrawingSection.FOREGROUND, 2))
            ringSprite.setSize(0.5f * ConstVals.PPM)
            spritesComponent.sprites.put("ring_$i", ringSprite)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(sawRegion!!, 1, 2, 0.1f, true)
        val animator = Animator(animation)

        val animationsComponent = AnimationsComponent()
        animationsComponent.putAnimator("saw", sprites["saw"], animator)
        return animationsComponent
    }
}
