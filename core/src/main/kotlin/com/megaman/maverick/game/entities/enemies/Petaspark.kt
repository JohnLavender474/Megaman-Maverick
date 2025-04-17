package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

// TODO: Currently a failed experiment, but might be worth revisiting...
class Petaspark(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "Petaspark"

        private const val MOVE_DUR = 2f
        private const val SHOCK_DUR = 0.5f
        private const val SHOCK_TIME = 0.25f

        private const val VEL_X = 2.5f
        private const val VEL_Y = 2.5f
        private const val GRAVITY = -0.1f

        private val animDefs = orderedMapOf(
            PetasparkState.MOVE pairTo AnimationDef(2, 2, 0.1f, true),
            PetasparkState.SHOCK pairTo AnimationDef(2, 1, 0.1f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class PetasparkState { MOVE, SHOCK }

    override lateinit var facing: Facing

    private val loop = Loop(PetasparkState.MOVE, PetasparkState.SHOCK)
    private val currentState: PetasparkState
        get() = loop.getCurrent()
    private val timers = orderedMapOf(
        PetasparkState.MOVE pairTo Timer(MOVE_DUR),
        PetasparkState.SHOCK pairTo Timer(SHOCK_DUR)
            .addRunnable(TimeMarkedRunnable(SHOCK_TIME) { shock() })
    )

    private var onWall = false
    private var wasOnWall = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            animDefs.keys().forEach { state ->
                val key = state.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.positionOnPoint(spawn, Position.BOTTOM_CENTER)

        facing = if (game.megaman.body.getX() > body.getX()) Facing.RIGHT else Facing.LEFT

        onWall = false
        wasOnWall = false

        loop.reset()
        timers.values().forEach { it.reset() }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            wasOnWall = onWall
            onWall = (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT))

            if (!megaman.dead && body.isSensing(BodySense.FEET_ON_GROUND)) when {
                megaman.body.getBounds().getPositionPoint(Position.BOTTOM_RIGHT).x < body.getX() ->
                    facing = Facing.LEFT

                megaman.body.getX() > body.getBounds().getPositionPoint(Position.BOTTOM_RIGHT).x ->
                    facing = Facing.RIGHT
            }

            val timer = timers[currentState]
            timer.update(delta)
            if (timer.isFinished() && body.isSensingAny(
                    BodySense.FEET_ON_GROUND,
                    BodySense.SIDE_TOUCHING_BLOCK_LEFT,
                    BodySense.SIDE_TOUCHING_BLOCK_RIGHT
                )
            ) {
                loop.next()
                timer.reset()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(2f * ConstVals.PPM, 1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(
            body,
            FixtureType.BODY,
            GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
        )
        bodyFixture.putProperty(ConstKeys.GRAVITY_ROTATABLE, false)
        body.addFixture(bodyFixture)
        bodyFixture.drawingColor = Color.BLUE
        debugShapes.add { bodyFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val leftFixture =
            Fixture(
                body,
                FixtureType.SIDE,
                GameRectangle().setSize(ConstVals.PPM / 32f, ConstVals.PPM.toFloat())
            )
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        // leftFixture.offsetFromBodyAttachment.y = 0.25f * ConstVals.PPM
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.ORANGE
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(
            body,
            FixtureType.SIDE,
            GameRectangle().setSize(0.1f * ConstVals.PPM, ConstVals.PPM.toFloat())
        )
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        // rightFixture.offsetFromBodyAttachment.y = 0.25f * ConstVals.PPM
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.ORANGE
        debugShapes.add { rightFixture }

        val damageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
        )
        body.addFixture(damageableFixture)

        val damagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat()),
        )
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y =
                if (body.isSensing(BodySense.FEET_ON_GROUND)) 0f else GRAVITY * ConstVals.PPM

            when (currentState) {
                PetasparkState.MOVE -> when {
                    onWall -> {
                        if (!wasOnWall) body.physics.velocity.x = 0f
                        body.physics.velocity.y = VEL_Y * ConstVals.PPM
                    }

                    else -> {
                        if (wasOnWall) body.translate(0f, ConstVals.PPM / 10f)
                        body.physics.velocity.x = VEL_X * ConstVals.PPM * facing.value
                    }
                }

                PetasparkState.SHOCK -> body.physics.velocity.x = 0f
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(4f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = when {
                onWall -> if (facing == Facing.LEFT) Position.CENTER_LEFT else Position.CENTER_RIGHT
                else -> Position.BOTTOM_CENTER
            }
            val bodyPosition = when {
                onWall -> when (position) {
                    Position.CENTER_LEFT -> body.getPositionPoint(Position.CENTER_LEFT)
                    else -> body.getPositionPoint(Position.CENTER_RIGHT)
                }

                else -> body.getPositionPoint(Position.BOTTOM_CENTER)
            }
            sprite.setPosition(bodyPosition, position)

            sprite.setOriginCenter()
            sprite.rotation = when {
                onWall -> if (facing == Facing.LEFT) -90f else 90f
                else -> 0f
            }

            sprite.setFlip(isFacing(Facing.LEFT), false)

            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { currentState.name.lowercase() }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key.name.lowercase()
                        val (rows, columns, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                    }
                }
                .build()
        )
        .build()

    private fun shock() {

    }
}
