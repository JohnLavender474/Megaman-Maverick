package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.systems.GameSystem
import com.maverick.game.engine.updatables.UpdatablesSystem
import com.maverick.game.engine.world.WorldSystem
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.maverick.game.engine.world.contacts.ContactRuleDispatcher
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.MegaEntityFactory
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
private const val FRAME_SIZE = 16
private const val SPEED = 8f * PPM
private const val DRAW_STEP = 11.25f
private const val SPRITE_ANGLE_OFFSET = 270f
private const val ROUNDING_OFFSET = 4f

private data class NeedleDrawCase(val step: Int, val frame: Int, val flipX: Boolean, val rotation: Float)

private val DRAW_CASES = (0 until 32).map { step ->
    val quadrant = step / 8
    val stepInQuadrant = step % 8
    val mirrored = stepInQuadrant > 4
    NeedleDrawCase(
        step = step,
        frame = if (mirrored) 8 - stepInQuadrant else stepInQuadrant,
        flipX = mirrored,
        rotation = quadrant * 90f + if (mirrored) 90f else 0f
    )
}

class NeedleTest : DescribeSpec({

    lateinit var engine: GameEngine
    lateinit var services: MegaServices
    fun step() = engine.update(STEP)

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
        every { atlas.findRegion(any()) } returns AtlasRegion(texture, 0, 0, FRAME_SIZE, FRAME_SIZE)
        every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
        MegaEntityFactory.init(services)
    }

    afterEach {
        engine.reset()
        MegaEntityFactory.reset()
        clearAllMocks(answers = false)
    }

    fun shoot(velocityAngle: Float): Needle {
        val needle = MegaEntityFactory.fetch(Needle::class)!!
        needle.spawn(
            props(
                ConstKeys.POSITION pairTo Vector2(0f, 0f),
                ConstKeys.IMPULSE pairTo Vector2(SPEED, 0f).rotateDeg(velocityAngle)
            )
        ) shouldBe true
        return needle
    }

    describe("the draw direction") {
        DRAW_CASES.forEach { case ->
            val spriteAngle = case.step * DRAW_STEP
            it("at the sprite angle $spriteAngle (+/- $ROUNDING_OFFSET) is frame ${case.frame}, flip ${case.flipX}, turned ${case.rotation}") {
                val direction = NeedleDrawDirection()
                listOf(ROUNDING_OFFSET, -ROUNDING_OFFSET, ROUNDING_OFFSET + 360f).forEach { offset ->
                    direction.set(spriteAngle + offset)

                    direction.frame shouldBe case.frame
                    direction.flipX shouldBe case.flipX
                    direction.rotation shouldBe (case.rotation plusOrMinus 0.001f)
                }
            }
        }
    }

    describe("the draw of a needle") {
        DRAW_CASES.forEach { case ->
            val spriteAngle = case.step * DRAW_STEP
            it("flying at the sprite angle $spriteAngle (+/- $ROUNDING_OFFSET) draws flip ${case.flipX}, turned ${case.rotation}") {
                listOf(ROUNDING_OFFSET, -ROUNDING_OFFSET).forEach { offset ->
                    val needle = shoot(spriteAngle + offset - SPRITE_ANGLE_OFFSET)
                    step()

                    needle.getComponent(SpritesComponent::class)!!.preProcess(0f)
                    val sprite = needle.sprites[SpritesComponent.DEFAULT_KEY]

                    sprite.isFlipX shouldBe case.flipX
                    sprite.isFlipY shouldBe false
                    sprite.rotation shouldBe (case.rotation plusOrMinus 0.001f)

                    needle.destroy()
                    step()
                }
            }
        }

        it("does not change the velocity") {
            val needle = shoot(37f)
            step()

            val velocity = needle.body.physics.velocity
            velocity.len() shouldBe (SPEED plusOrMinus 0.01f)
            velocity.angleDeg() shouldBe (37f plusOrMinus 0.01f)
        }
    }
})
