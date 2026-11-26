package com.megaman.maverick.game.entities.megaman

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.animations.IAnimator
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.megaman.components.MEGAMAN_SPRITE_KEY
import com.megaman.maverick.game.entities.megaman.constants.MegaChargeStatus
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.megaman.handlers.MegamanWeaponsHandler
import com.megaman.maverick.game.entities.megaman.weapons.AxeThrowWindup
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.state.FreezeComponent
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.PlayPhaseComponent
import com.megaman.maverick.game.entities.state.TagsComponent
import com.megaman.maverick.game.players.PlayerCharacter
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify

private const val AXE_KEY = "stand_axe_throw_axeswinger"

class MegamanDeathCleanupTest : DescribeSpec({

    afterEach {
        MegaEntityFactory.reset()
        unmockkAll()
    }

    describe("the death of Megaman") {
        it("resets the weapons handler, so the spawned sets and the cooldowns do not survive the death") {
            val services = testServices(engine = mockk<GameEngine>(relaxed = true))
            val megaman = Megaman(services)
            megaman.addComponent(TagsComponent(Megaman.TAG))
            services.players.register(PlayerSlot(0), megaman)
            val handler = mockk<MegamanWeaponsHandler>(relaxed = true)
            megaman.weaponsHandler = handler

            megaman.onDestroy()

            verify(exactly = 1) { handler.reset() }
        }
    }

    describe("the axe throw") {
        lateinit var engine: GameEngine
        lateinit var services: MegaServices
        lateinit var handler: MegamanWeaponsHandler
        lateinit var windup: AxeThrowWindup
        lateinit var animation: IAnimation
        var existence = EntityExistenceState.LIVE

        fun update(delta: Float) = services.updatables.values().forEach { it.update(delta) }

        fun fire() = handler.fireWeapon(MegamanWeapons.AXE_SWINGER, MegaChargeStatus.NOT_CHARGED) shouldBe true

        beforeEach {
            engine = mockk(relaxed = true)
            val gameState = GameState().also { it.addWeapon(PlayerCharacter.MEGAMAN, MegamanWeapons.AXE_SWINGER) }
            services = testServices(engine = engine, gameState = gameState)
            MegaEntityFactory.init(services)
            existence = EntityExistenceState.LIVE
            windup = AxeThrowWindup(MegamanValues.AXE_HOLD_TIME)
            animation = mockk(relaxed = true)
            val animator = object : Animator(keySupplier = { AXE_KEY }, animations = objectMapOf(AXE_KEY pairTo animation)) {
                init {
                    currentKey = AXE_KEY
                }
            }
            val animations = AnimationsComponent(
                orderedMapOf<Any, IAnimator>(MEGAMAN_SPRITE_KEY pairTo animator),
                OrderedMap<Any, GameSprite>()
            )

            val megaman = mockk<Megaman>(relaxed = true)
            every { megaman.services } returns services
            every { megaman.character } returns PlayerCharacter.MEGAMAN
            every { megaman.existenceState } answers { existence }
            every { megaman.getComponent(PlayPhaseComponent::class) } returns PlayPhaseComponent(PlayPhase.IN_PLAY)
            every { megaman.getComponent(FreezeComponent::class) } returns null
            every { megaman.getComponent(AnimationsComponent::class) } returns animations
            every { megaman.axeWindup } returns windup
            every { megaman.direction } returns Direction.UP
            every { megaman.facing } returns Facing.RIGHT
            every { megaman.body } returns Body(BodyType.DYNAMIC)
            handler = MegamanWeaponsHandler(megaman).also { it.reset() }
        }

        it("throws the axe after the 0.1 s windup on a tap") {
            fire()

            update(MegamanValues.AXE_WINDUP_TIME - 0.01f)
            verify(exactly = 0) { engine.spawn(ofType<Axe>(), any()) }

            update(0.02f)
            verify(exactly = 1) { engine.spawn(ofType<Axe>(), any()) }
        }

        it("throws no axe when the player dies before the windup ends") {
            fire()

            update(0.05f)
            existence = EntityExistenceState.DESTROY_QUEUED
            update(0.15f)

            verify(exactly = 0) { engine.spawn(ofType<Axe>(), any()) }
        }

        it("throws at once with the curve impulse after a hold of at least the windup time") {
            windup.hold(0.4f)
            val spawnProps = slot<Properties>()

            fire()

            verify(exactly = 1) { engine.spawn(ofType<Axe>(), capture(spawnProps)) }
            val impulse = spawnProps.captured.get(ConstKeys.IMPULSE, Vector2::class)!!
            impulse.x shouldBe (8f * ConstVals.PPM plusOrMinus 0.01f)
            impulse.y shouldBe (20f * ConstVals.PPM plusOrMinus 0.01f)
        }

        it("throws at once with the full impulse after the full hold") {
            windup.hold(MegamanValues.AXE_HOLD_TIME)
            val spawnProps = slot<Properties>()

            fire()

            verify(exactly = 1) { engine.spawn(ofType<Axe>(), capture(spawnProps)) }
            val impulse = spawnProps.captured.get(ConstKeys.IMPULSE, Vector2::class)!!
            impulse.x shouldBe (MegamanValues.AXE_IMPULSE_X_MAX * ConstVals.PPM plusOrMinus 0.01f)
            impulse.y shouldBe (MegamanValues.AXE_IMPULSE_Y_MAX * ConstVals.PPM plusOrMinus 0.01f)
        }

        it("jumps the held axe throw animation to its throw frame on an immediate throw") {
            windup.hold(MegamanValues.AXE_WINDUP_TIME)

            fire()

            verify(exactly = 1) { animation.setCurrentTime(MegamanValues.AXE_WINDUP_TIME) }
        }

        it("throws after the rest of the windup after a hold shorter than the windup time") {
            windup.hold(0.05f)

            fire()
            verify(exactly = 0) { engine.spawn(ofType<Axe>(), any()) }

            update(0.04f)
            verify(exactly = 0) { engine.spawn(ofType<Axe>(), any()) }

            update(0.02f)
            verify(exactly = 1) { engine.spawn(ofType<Axe>(), any()) }
            verify(exactly = 0) { animation.setCurrentTime(any()) }
        }

        it("throws no axe when the player dies during the rest of the windup") {
            windup.hold(0.05f)

            fire()
            update(0.02f)
            existence = EntityExistenceState.DESTROY_QUEUED
            update(0.05f)

            verify(exactly = 0) { engine.spawn(ofType<Axe>(), any()) }
        }
    }
})
