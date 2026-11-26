package com.megaman.maverick.game.entities.megaman.weapons

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.audio.AudioComponent
import com.maverick.game.engine.behaviors.BehaviorsComponent
import com.maverick.game.engine.camera.CameraRotation
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.points.Points
import com.maverick.game.engine.points.PointsComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.enemies.MetaWheelF
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.addRuleComponents
import com.megaman.maverick.game.entities.megaman.components.AXE_THROW_PREVIEW_SPRITE_KEY_PREFIX
import com.megaman.maverick.game.entities.megaman.components.defineAxeThrowPreviewSprites
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.projectiles.AxeTrajectory
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.RunComponent
import com.megaman.maverick.game.entities.state.TagsComponent
import com.megaman.maverick.game.entities.state.TeleportableComponent
import com.megaman.maverick.game.entities.state.WeaponWindupComponent
import com.megaman.maverick.game.entities.state.setPlayPhase
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.players.windups
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.getCenter
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

private const val PPM = ConstVals.PPM.toFloat()
private const val SHOWN_HOLD = 0.4f

class AxeThrowPreviewTest : DescribeSpec({

    lateinit var services: MegaServices
    lateinit var rotation: CameraRotation
    lateinit var megaman: Megaman
    lateinit var preview: AxeThrowPreview

    fun hold(time: Float) = megaman.windups.hold(megaman.axeWindup, time)

    fun hurt() {
        val damager = MetaWheelF(megaman.services).also { it.addComponent(TagsComponent(MetaWheelF.TAG)) }
        megaman.takeDamageFrom(damager) shouldBe true
    }

    beforeEach {
        rotation = CameraRotation()
        services = testServices(
            engine = mockk<GameEngine>(relaxed = true),
            gameCamera = CameraView(OrthographicCamera(), rotation)
        )
        services.levelState.currentLevel = LevelDefinition.DESERT_MAN

        megaman = Megaman(services)
        megaman.existenceState = EntityExistenceState.LIVE
        megaman.weaponsHandler = mockk(relaxed = true)
        megaman.jetpackHandler = mockk(relaxed = true)
        megaman.currentWeapon = MegamanWeapons.AXE_SWINGER
        megaman.facing = Facing.RIGHT

        megaman.addComponent(BehaviorsComponent())
        megaman.addRuleComponents()
        megaman.addComponent(megaman.defineBodyComponent())
        megaman.body.setCenter(4f * PPM, 2f * PPM)
        megaman.addComponent(TeleportableComponent())
        megaman.addComponent(RunComponent())
        megaman.addComponent(megaman.defineMomentumComponent())
        megaman.addComponent(AudioComponent())
        megaman.addComponent(
            PointsComponent(ConstKeys.HEALTH pairTo Points(0, ConstVals.MAX_HEALTH, ConstVals.MAX_HEALTH))
        )
        megaman.addComponent(WeaponWindupComponent())
        services.players.register(PlayerSlot(0), megaman)

        preview = megaman.axeThrowPreview
    }

    afterEach { unmockkAll() }

    describe("the shown state") {
        it("matches the dots of the launch values of the current hold") {
            hold(SHOWN_HOLD)

            preview.update(megaman)

            val position = Vector2()
            val impulse = Vector2()
            val gravity = Vector2()
            AxeThrow.launch(megaman, megaman.axeWindup.time, position, impulse)
            AxeTrajectory.gravity(Direction.UP, 1f, gravity)
            val expected = Array<Vector2>()
            repeat(MegamanValues.AXE_PREVIEW_MAX_DOTS) { expected.add(Vector2()) }
            val expectedCount = AxeTrajectory.dots(
                start = position,
                velocity = impulse,
                gravity = gravity,
                up = Direction.UP,
                fixedStep = services.settings.performance.fixedStep,
                spacing = MegamanValues.AXE_PREVIEW_DOT_SPACING * PPM,
                maxDots = MegamanValues.AXE_PREVIEW_MAX_DOTS,
                maxTime = MegamanValues.AXE_PREVIEW_MAX_TIME,
                out = expected
            )

            (preview.count > 0) shouldBe true
            preview.count shouldBe expectedCount
            for (i in 0 until expectedCount) {
                preview.points[i].x shouldBe (expected[i].x plusOrMinus 0.001f)
                preview.points[i].y shouldBe (expected[i].y plusOrMinus 0.001f)
            }
        }

        it("starts the arc above and ahead of Mega Man, in his facing") {
            hold(SHOWN_HOLD)

            preview.update(megaman)

            val center = megaman.body.getCenter()
            (preview.points[0].x > center.x) shouldBe true
            (preview.points[0].y > center.y) shouldBe true
        }

        it("keeps its own vectors across updates") {
            val before = Array<Vector2>()
            preview.points.forEach { before.add(it) }
            hold(SHOWN_HOLD)

            preview.update(megaman)
            hold(0.1f)
            preview.update(megaman)

            preview.points.size shouldBe MegamanValues.AXE_PREVIEW_MAX_DOTS
            for (i in 0 until before.size) (preview.points[i] === before[i]) shouldBe true
        }
    }

    describe("the hidden state") {
        it("has no dots with no hold") {
            preview.update(megaman)

            preview.count shouldBe 0
        }

        it("has no dots before the delay, and dots after it") {
            hold(MegamanValues.AXE_PREVIEW_DELAY - 0.05f)
            preview.update(megaman)
            preview.count shouldBe 0

            hold(0.06f)
            preview.update(megaman)
            (preview.count > 0) shouldBe true
        }

        it("has no dots out of play") {
            hold(SHOWN_HOLD)
            megaman.setPlayPhase(PlayPhase.PRE_PLAY)

            preview.update(megaman)

            preview.count shouldBe 0
        }

        it("has no dots during a room transition") {
            hold(SHOWN_HOLD)
            services.levelState.roomTransition = true

            preview.update(megaman)

            preview.count shouldBe 0
        }

        it("has no dots during a camera turn") {
            hold(SHOWN_HOLD)
            rotation.start(Direction.LEFT, 1f)

            preview.update(megaman)

            preview.count shouldBe 0
        }

        it("has no dots while damaged") {
            hold(SHOWN_HOLD)
            hurt()

            preview.update(megaman)

            preview.count shouldBe 0
        }

        it("drops the dots when the hold stops") {
            hold(SHOWN_HOLD)
            preview.update(megaman)

            megaman.windups.stop()
            preview.update(megaman)

            preview.count shouldBe 0
        }
    }

    describe("the dot sprites") {
        lateinit var sprites: SpritesComponent

        beforeEach {
            val atlas = mockk<TextureAtlas>()
            every { atlas.findRegion(any()) } returns AtlasRegion(mockk<Texture>(relaxed = true), 0, 0, 4, 4)
            every { services.assets.get(any<String>(), TextureAtlas::class.java) } returns atlas
            sprites = SpritesComponent()
            megaman.defineAxeThrowPreviewSprites(sprites)
        }

        fun dot(i: Int) = sprites.sprites["$AXE_THROW_PREVIEW_SPRITE_KEY_PREFIX$i"]!!

        it("puts one sprite per dot") {
            sprites.sprites.size shouldBe MegamanValues.AXE_PREVIEW_MAX_DOTS
        }

        it("shows the first count dots at their points, fading, and hides the rest") {
            hold(SHOWN_HOLD)
            preview.update(megaman)
            val count = preview.count

            sprites.preProcess(0f)

            for (i in 0 until MegamanValues.AXE_PREVIEW_MAX_DOTS) dot(i).hidden shouldBe (i >= count)
            val first = dot(0)
            (first.x + first.width / 2f) shouldBe (preview.points[0].x plusOrMinus 0.001f)
            (first.y + first.height / 2f) shouldBe (preview.points[0].y plusOrMinus 0.001f)
            first.color.a shouldBe (MegamanValues.AXE_PREVIEW_FIRST_DOT_ALPHA plusOrMinus 0.01f)
            dot(count - 1).color.a shouldBe (MegamanValues.AXE_PREVIEW_LAST_DOT_ALPHA plusOrMinus 0.01f)
        }

        it("hides every dot with no hold") {
            preview.update(megaman)

            sprites.preProcess(0f)

            for (i in 0 until MegamanValues.AXE_PREVIEW_MAX_DOTS) dot(i).hidden shouldBe true
        }
    }
})
