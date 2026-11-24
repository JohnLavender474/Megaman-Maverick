package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.interfaces.IDirectional
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.updatables.UpdatablesSystem
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.contacts.ContactRuleDispatcher
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.ChargedShotResidual
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.collisions.MegaCollisionHandler
import com.megaman.maverick.game.world.contacts.MegaContactFilter
import com.megaman.maverick.game.world.contacts.megaContactRules
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

private const val PPM = ConstVals.PPM.toFloat()
private const val STEP = 1f / 150f
private const val VIEW_SIZE = 64f
private const val TEXTURE_SIZE = 1024
private const val SPEED = MegamanValues.BULLET_VEL * PPM
private const val OVERLAP = 2f
private const val BLOCK_DEPTH = 2f * PPM
private const val BLOCK_SPAN = 4f * PPM

private class TestShooterPlayer(services: MegaServices) : MegaGameEntity(services), IBodyEntity, IFaceable,
    IDirectional, IPlayer {

    override var direction = Direction.UP
    override var facing = Facing.RIGHT

    init {
        addComponent(BodyComponent(Body(BodyType.DYNAMIC, width = PPM, height = 1.5f * PPM)))
    }

    override fun getType() = EntityType.MEGAMAN
}

private data class BounceCase(val gravity: Direction, val motion: Direction, val shotDown: Boolean, val angle: Float)

private val BOUNCE_CASES = listOf(
    BounceCase(Direction.UP, Direction.UP, false, 270f),
    BounceCase(Direction.UP, Direction.UP, true, 315f),
    BounceCase(Direction.UP, Direction.DOWN, false, 90f),
    BounceCase(Direction.UP, Direction.DOWN, true, 45f),
    BounceCase(Direction.UP, Direction.LEFT, false, 45f),
    BounceCase(Direction.UP, Direction.LEFT, true, 45f),
    BounceCase(Direction.UP, Direction.RIGHT, false, 135f),
    BounceCase(Direction.UP, Direction.RIGHT, true, 135f),
    BounceCase(Direction.DOWN, Direction.UP, false, 270f),
    BounceCase(Direction.DOWN, Direction.UP, true, 225f),
    BounceCase(Direction.DOWN, Direction.DOWN, false, 90f),
    BounceCase(Direction.DOWN, Direction.DOWN, true, 135f),
    BounceCase(Direction.DOWN, Direction.LEFT, false, 315f),
    BounceCase(Direction.DOWN, Direction.LEFT, true, 315f),
    BounceCase(Direction.DOWN, Direction.RIGHT, false, 225f),
    BounceCase(Direction.DOWN, Direction.RIGHT, true, 225f),
    BounceCase(Direction.LEFT, Direction.UP, false, 225f),
    BounceCase(Direction.LEFT, Direction.UP, true, 225f),
    BounceCase(Direction.LEFT, Direction.DOWN, false, 135f),
    BounceCase(Direction.LEFT, Direction.DOWN, true, 135f),
    BounceCase(Direction.LEFT, Direction.LEFT, false, 0f),
    BounceCase(Direction.LEFT, Direction.LEFT, true, 45f),
    BounceCase(Direction.LEFT, Direction.RIGHT, false, 180f),
    BounceCase(Direction.LEFT, Direction.RIGHT, true, 135f),
    BounceCase(Direction.RIGHT, Direction.UP, false, 315f),
    BounceCase(Direction.RIGHT, Direction.UP, true, 315f),
    BounceCase(Direction.RIGHT, Direction.DOWN, false, 45f),
    BounceCase(Direction.RIGHT, Direction.DOWN, true, 45f),
    BounceCase(Direction.RIGHT, Direction.LEFT, false, 0f),
    BounceCase(Direction.RIGHT, Direction.LEFT, true, 315f),
    BounceCase(Direction.RIGHT, Direction.RIGHT, false, 180f),
    BounceCase(Direction.RIGHT, Direction.RIGHT, true, 225f)
)

class ChargedShotTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    lateinit var player: TestShooterPlayer

    fun step(cycles: Int = 1) = repeat(cycles) { engine.update(STEP) }

    beforeEach {
        val players = MegaPlayers()
        val gravity = WorldGravity(players)
        val worldContainer = SimpleGridWorldContainer(ConstVals.PPM)
        val worldSystem = WorldSystem(
            ppm = ConstVals.PPM,
            fixedStep = STEP,
            worldContainerSupplier = { worldContainer },
            contactListener = ContactRuleDispatcher(megaContactRules(players, gravity, GameMode.SINGLE.rules) { _, _ -> false }),
            collisionHandler = MegaCollisionHandler(),
            contactFilter = MegaContactFilter()
        )
        engine = GameEngine(Array<GameSystem>().apply {
            add(worldSystem)
            add(UpdatablesSystem())
        })
        services = testServices(
            engine = engine,
            players = players,
            gravity = gravity,
            gameCamera = CameraView(
                OrthographicCamera().apply {
                    viewportWidth = VIEW_SIZE * PPM
                    viewportHeight = VIEW_SIZE * PPM
                }
            )
        )
        val texture = mockk<Texture>(relaxed = true)
        every { texture.width } returns TEXTURE_SIZE
        every { texture.height } returns TEXTURE_SIZE
        val atlas = mockk<TextureAtlas>()
        every { atlas.findRegion(any()) } returns AtlasRegion(texture, 0, 0, 64, 64)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        MegaEntityFactory.init(services)

        player = TestShooterPlayer(services)
        player.body.setCenter(-20f * PPM, -20f * PPM)
        players.register(PlayerSlot(0), player)
    }

    afterEach {
        engine.reset()
        MegaEntityFactory.reset()
        clearAllMocks(answers = false)
    }

    fun shoot(trajectory: Vector2, shotDown: Boolean = false): ChargedShot {
        val shot = MegaEntityFactory.fetch(ChargedShot::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo player,
                ConstKeys.POSITION pairTo Vector2(0f, 0f),
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.BOOLEAN pairTo true,
                "${ConstKeys.SHOOT}_${ConstKeys.DOWN}" pairTo shotDown
            )
        ) shouldBe true
        return shot
    }

    fun blockAhead(motion: Direction) {
        val halfWidth = ChargedShot.FULL_BODY_SIZE.x / 2f
        val halfHeight = ChargedShot.FULL_BODY_SIZE.y / 2f
        val bounds = when (motion) {
            Direction.RIGHT -> GameRectangle(halfWidth - OVERLAP, -BLOCK_SPAN / 2f, BLOCK_DEPTH, BLOCK_SPAN)
            Direction.LEFT -> GameRectangle(-halfWidth + OVERLAP - BLOCK_DEPTH, -BLOCK_SPAN / 2f, BLOCK_DEPTH, BLOCK_SPAN)
            Direction.UP -> GameRectangle(-BLOCK_SPAN / 2f, halfHeight - OVERLAP, BLOCK_SPAN, BLOCK_DEPTH)
            Direction.DOWN -> GameRectangle(-BLOCK_SPAN / 2f, -halfHeight + OVERLAP - BLOCK_DEPTH, BLOCK_SPAN, BLOCK_DEPTH)
        }
        Block(services).spawn(props(ConstKeys.BOUNDS pairTo bounds)) shouldBe true
    }

    fun axisTrajectory(motion: Direction) = when (motion) {
        Direction.RIGHT -> Vector2(SPEED, 0f)
        Direction.LEFT -> Vector2(-SPEED, 0f)
        Direction.UP -> Vector2(0f, SPEED)
        Direction.DOWN -> Vector2(0f, -SPEED)
    }

    describe("a bounce off a block") {
        BOUNCE_CASES.forEach { case ->
            val shotDownText = if (case.shotDown) "shot down from a jump" else "not shot down"
            it("gravity ${case.gravity}, moving ${case.motion}, $shotDownText: leaves at ${case.angle} with the same speed") {
                player.direction = case.gravity
                blockAhead(case.motion)
                step()
                val shot = shoot(axisTrajectory(case.motion), case.shotDown)

                step()

                val velocity = shot.body.physics.velocity
                velocity.len() shouldBe (SPEED plusOrMinus 0.01f)
                velocity.angleDeg() shouldBe (case.angle plusOrMinus 0.01f)
            }
        }
    }

    describe("a spawn trajectory") {
        Direction.entries.forEach { motion ->
            it("on the axis $motion stays exactly the same") {
                val trajectory = axisTrajectory(motion)
                val shot = shoot(trajectory.cpy())

                step()

                shot.body.physics.velocity shouldBe trajectory
            }
        }

        it("off the axis turns to the nearest multiple of 45 with the same speed") {
            val shot = shoot(Vector2(SPEED, 0f).rotateDeg(30f))

            step()

            val velocity = shot.body.physics.velocity
            velocity.len() shouldBe (SPEED plusOrMinus 0.01f)
            velocity.angleDeg() shouldBe (45f plusOrMinus 0.01f)
        }
    }

    fun drawRotation(entity: ISpritesEntity, key: Any = SpritesComponent.DEFAULT_KEY): Float {
        entity.getComponent(SpritesComponent::class)!!.preProcess(0f)
        return entity.sprites[key].rotation
    }

    fun drawTurnFor(angle: Float) = if (angle % 90f == 0f) angle else angle - 45f

    describe("the draw turn") {
        BOUNCE_CASES.forEach { case ->
            it("of the shot after the bounce: gravity ${case.gravity}, moving ${case.motion}, shot down ${case.shotDown}") {
                player.direction = case.gravity
                blockAhead(case.motion)
                step()
                val shot = shoot(axisTrajectory(case.motion), case.shotDown)
                step()

                drawRotation(shot, ChargedShot.TAG) shouldBe (drawTurnFor(case.angle) plusOrMinus 0.001f)
            }
        }

        listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f).forEach { angle ->
            it("of the explosion and the residual at $angle is ${drawTurnFor(angle)}") {
                val position = Vector2(0f, 0f)
                val explosion = MegaEntityFactory.fetch(ChargedShotExplosion::class)!!
                explosion.spawn(
                    props(
                        ConstKeys.OWNER pairTo player,
                        ConstKeys.BOOLEAN pairTo true,
                        ConstKeys.POSITION pairTo position,
                        ConstKeys.ROTATION pairTo angle + 10f
                    )
                ) shouldBe true
                val residual = MegaEntityFactory.fetch(ChargedShotResidual::class)!!
                residual.spawn(
                    props(
                        ConstKeys.BOOLEAN pairTo false,
                        ConstKeys.POSITION pairTo position,
                        ConstKeys.ROTATION pairTo angle - 10f
                    )
                ) shouldBe true
                step()

                drawRotation(explosion) shouldBe (drawTurnFor(angle) plusOrMinus 0.001f)
                drawRotation(residual) shouldBe (drawTurnFor(angle) plusOrMinus 0.001f)
            }
        }
    }
})
