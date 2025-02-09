package com.megaman.maverick.game.entities.bosses.bigassrobot

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class BigAssMaverickRobot(game: MegamanMaverickGame) : AbstractBoss(game) {

    companion object {
        const val TAG = "BigAssMaverickRobot"

        private const val BODY_WIDTH = 7f
        private const val BODY_HEIGHT = 10f

        private const val HEAD_SIZE = 3f

        private const val INIT_DUR = 1f
        private const val SHOOT_ORB_DELAY = 3f
        private const val THROW_HAND_DELAY = 3f

        private const val HAND_OFFSET_X = 2f
        private const val HAND_OFFSET_Y = 0f
        private const val HAND_ROTATION_SPEED = 5f
    }

    private val initTimer = Timer(INIT_DUR)
    private val shootOrbDelay = Timer(SHOOT_ORB_DELAY)
    private val throwHandDelay = Timer(THROW_HAND_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        initTimer.reset()
        shootOrbDelay.reset()
        throwHandDelay.reset()
    }

    override fun isReady(delta: Float) = initTimer.isFinished()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            initTimer.update(delta)
            if (!initTimer.isFinished()) return@add


        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val headDamageable = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(HEAD_SIZE * ConstVals.PPM))
        headDamageable.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headDamageable)
        headDamageable.drawingColor = Color.PURPLE
        debugShapes.add { headDamageable }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    // TODO
    override fun defineSpritesComponent() = SpritesComponentBuilder().build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder().build()
}

