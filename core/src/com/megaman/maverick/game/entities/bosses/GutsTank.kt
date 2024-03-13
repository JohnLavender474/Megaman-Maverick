package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.getPosition
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.world.*
import kotlin.reflect.KClass

class GutsTank(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IParentEntity {

    enum class GutsTankMoveState {
        PAUSE, MOVE,
    }

    enum class GutsTankAttackState {
        LAUNCH_FIST, CHUCK_BULLETS_FROM_MOUTH, SHOOT_BLASTS_FROM_MOUTH
    }

    companion object {
        const val TAG = "GutsTank"
        private const val BODY_WIDTH = 8f
        private const val BODY_HEIGHT = 4.46875f
        private const val TANK_BLOCK_HEIGHT = 2.0365f
        private const val BODY_BLOCK_WIDTH = 5.48625f
        private const val BODY_BLOCK_HEIGHT = 12f
        private const val X_VEL = 2f
        private const val MOVEMENT_PAUSE_DUR = 2f
        private var mouthOpenRegion: TextureRegion? = null
        private var mouthClosedRegion: TextureRegion? = null
        private var fistRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    override var children = Array<IGameEntity>()

    private val attackStates = ObjectSet<GutsTankAttackState>()
    private val movementPauseTimer = Timer(MOVEMENT_PAUSE_DUR)

    private lateinit var frontPoint: Vector2
    private lateinit var backPoint: Vector2
    private lateinit var tankBlockOffset: Vector2
    private lateinit var bodyBlockOffset: Vector2
    private lateinit var moveState: GutsTankMoveState

    private var tankBlock: Block? = null
    private var bodyBlock: Block? = null
    private var moveToFront = true

    override fun init() {
        if (mouthOpenRegion == null || mouthClosedRegion == null || fistRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            mouthOpenRegion = atlas.findRegion("GutsTank/MouthOpen")
            mouthClosedRegion = atlas.findRegion("GutsTank/MouthClosed")
            fistRegion = atlas.findRegion("GutsTank/Fist")
        }
        super<AbstractBoss>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomLeftPoint()
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.setBottomLeftToPoint(spawn)

        tankBlock = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        val tankBlockBounds =
            GameRectangle(spawn.x, spawn.y, BODY_WIDTH * ConstVals.PPM, TANK_BLOCK_HEIGHT * ConstVals.PPM)
        game.gameEngine.spawn(
            tankBlock!!, props(
                ConstKeys.CULL_OUT_OF_BOUNDS to false,
                ConstKeys.BOUNDS to tankBlockBounds,
                ConstKeys.FIXTURE_LABELS to objectSetOf(FixtureLabel.NO_PROJECTILE_COLLISION),
                ConstKeys.FIXTURES to gdxArrayOf(
                    Pair(FixtureType.SHIELD, props(ConstKeys.DIRECTION to Direction.DOWN))
                )
            )
        )

        bodyBlock = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        val bodyBlockBounds = GameRectangle().setSize(
            BODY_BLOCK_WIDTH * ConstVals.PPM, BODY_BLOCK_HEIGHT * ConstVals.PPM
        ).setBottomRightToPoint(tankBlockBounds.getBottomRightPoint())
        game.gameEngine.spawn(
            bodyBlock!!, props(
                ConstKeys.CULL_OUT_OF_BOUNDS to false,
                ConstKeys.BOUNDS to bodyBlockBounds,
                ConstKeys.FIXTURE_LABELS to objectSetOf(
                    FixtureLabel.NO_SIDE_TOUCHIE,
                    FixtureLabel.NO_PROJECTILE_COLLISION
                ),
                ConstKeys.FIXTURES to gdxArrayOf(
                    Pair(FixtureType.SHIELD, props(ConstKeys.DIRECTION to Direction.UP))
                )
            )
        )

        tankBlockOffset = tankBlockBounds.getCenter().sub(body.getCenter())
        bodyBlockOffset = bodyBlockBounds.getCenter().sub(body.getCenter())

        frontPoint = spawnProps.get(ConstKeys.FRONT, RectangleMapObject::class)!!.rectangle.getPosition()
        backPoint = spawnProps.get(ConstKeys.BACK, RectangleMapObject::class)!!.rectangle.getPosition()

        moveState = GutsTankMoveState.MOVE
        moveToFront = true
        attackStates.clear()

        movementPauseTimer.reset()
    }

    override fun onDestroy() {
        super<AbstractBoss>.onDestroy()
        children.forEach { it.kill() }
        children.clear()

        tankBlock!!.body.fixtures.forEach { println(it) }
        tankBlock?.onDestroy()
        tankBlock = null

        bodyBlock?.onDestroy()
        bodyBlock = null
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!tankBlock!!.body.getCenter()
                    .epsilonEquals(body.getCenter().add(tankBlockOffset), 0.01f) ||
                !bodyBlock!!.body.getCenter()
                    .epsilonEquals(body.getCenter().add(bodyBlockOffset), 0.01f)
            ) {
                tankBlock!!.body.setCenter(body.getCenter().add(tankBlockOffset))
                bodyBlock!!.body.setCenter(body.getCenter().add(bodyBlockOffset))
            }

            when (moveState) {
                GutsTankMoveState.MOVE -> {
                    if (moveToFront) {
                        body.physics.velocity.x = -X_VEL * ConstVals.PPM
                        tankBlock!!.body.physics.velocity.x = -X_VEL * ConstVals.PPM
                        bodyBlock!!.body.physics.velocity.x = -X_VEL * ConstVals.PPM
                        if (body.x <= frontPoint.x) {
                            body.x = frontPoint.x
                            body.physics.velocity.x = 0f
                            moveState = GutsTankMoveState.PAUSE
                            moveToFront = false
                            movementPauseTimer.reset()
                        }
                    } else {
                        body.physics.velocity.x = X_VEL * ConstVals.PPM
                        tankBlock!!.body.physics.velocity.x = X_VEL * ConstVals.PPM
                        bodyBlock!!.body.physics.velocity.x = X_VEL * ConstVals.PPM
                        if (body.x >= backPoint.x) {
                            body.x = backPoint.x
                            body.physics.velocity.x = 0f
                            moveState = GutsTankMoveState.PAUSE
                            moveToFront = true
                            movementPauseTimer.reset()
                        }

                    }
                }

                GutsTankMoveState.PAUSE -> {
                    body.physics.velocity.x = 0f
                    tankBlock!!.body.physics.velocity.x = 0f
                    bodyBlock!!.body.physics.velocity.x = 0f
                    movementPauseTimer.update(delta)
                    if (movementPauseTimer.isFinished()) moveState = GutsTankMoveState.MOVE
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val damageableFixture = Fixture(GameRectangle().setSize(1f * ConstVals.PPM), FixtureType.DAMAGEABLE)
        damageableFixture.offsetFromBodyCenter.x = -2f * ConstVals.PPM
        damageableFixture.offsetFromBodyCenter.y = 2.5f * ConstVals.PPM
        body.addFixture(damageableFixture)
        damageableFixture.shape.color = Color.PURPLE
        debugShapes.add { damageableFixture.shape }

        val damagerFixture = Fixture(GameRectangle().setSize(1f * ConstVals.PPM), FixtureType.DAMAGER)
        damageableFixture.offsetFromBodyCenter.x = -2f * ConstVals.PPM
        damageableFixture.offsetFromBodyCenter.y = 2.5f * ConstVals.PPM
        body.addFixture(damageableFixture)
        damagerFixture.shape.color = Color.RED
        debugShapes.add { damagerFixture.shape }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        sprite.setSize(8f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, TAG to sprite)
        spritesComponent.putUpdateFunction(TAG) { _, _sprite ->
            _sprite as GameSprite
            _sprite.setPosition(body.getBottomLeftPoint(), Position.BOTTOM_LEFT)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            "mouth_closed"
        }
        val animations = objectMapOf<String, IAnimation>(
            "mouth_closed" to Animation(mouthClosedRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}