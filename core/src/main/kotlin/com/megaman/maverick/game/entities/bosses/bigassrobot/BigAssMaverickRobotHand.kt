package com.megaman.maverick.game.entities.bosses.bigassrobot

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.motion.RotatingLine
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.*

class BigAssMaverickRobotHand(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "BigAssMaverickRobotHand"

        private const val BODY_SIZE = 2f

        private const val BLOCK_WIDTH = 2f
        private const val BLOCK_HEIGHT = 0.25f

        private const val LAUNCH_SPEED = 8f
        private const val RETURN_SPEED = 4f
    }

    private lateinit var rotatingLine: RotatingLine
    private var block: Block? = null
    private var launched = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineBodyComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        setLaunched(false)

        val block = MegaEntityFactory.fetch(Block::class)!!
        block.spawn(
            props(
                ConstKeys.BLOCK_FILTERS pairTo TAG,
                ConstKeys.FIXTURE_LABELS pairTo objectSetOf(
                    FixtureLabel.NO_SIDE_TOUCHIE,
                    FixtureLabel.NO_PROJECTILE_COLLISION
                ),
                ConstKeys.WIDTH pairTo BODY_SIZE * ConstVals.PPM,
                ConstKeys.HEIGHT pairTo BODY_SIZE * ConstVals.PPM,
                ConstKeys.BODY_LABELS pairTo objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY)
            )
        )
        this.block = block

        val origin = spawnProps.get(ConstKeys.ORIGIN, Vector2::class)!!
        val radius = spawnProps.get(ConstKeys.RADIUS, Float::class)!!
        val speed = spawnProps.get(ConstKeys.SPEED, Float::class)!!
        rotatingLine = RotatingLine(origin.cpy(), radius, speed)
    }

    internal fun setLaunched(launched: Boolean) {
        this.launched = launched
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        block?.destroy()
        block = null
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->

    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(BODY_SIZE * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        val fixtureShape = GameCircle().setRadius(BODY_SIZE * ConstVals.PPM / 2f)

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.Companion.of(
                FixtureType.BODY pairTo fixtureShape.copy(),
                FixtureType.DAMAGER pairTo fixtureShape.copy()
            )
        )
    }

    // TODO
    private fun defineSpritesComponent() = SpritesComponentBuilder().build()

    override fun getType() = EntityType.OTHER

    override fun getTag() = TAG
}
