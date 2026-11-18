package com.megaman.maverick.game.screens.levels.respawn

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector3
import com.maverick.game.engine.GameEngine
import com.maverick.game.engine.camera.CameraRotation
import com.maverick.game.engine.camera.CameraView
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.locks.isLocked
import com.maverick.game.engine.players.PlayerSlot
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.container.SimpleGridWorldContainer
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.audio.MegaAudioManager
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.playPhase
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.TestFreezer
import com.megaman.maverick.game.players.TestHealthPlayer
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.screens.levels.camera.PartnerSpot
import com.megaman.maverick.game.screens.levels.camera.WarpSpotTest
import com.megaman.maverick.game.state.GameState
import com.megaman.maverick.game.testServices
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

private class RespawnRig(lives: Int = 3) {
    val engine = mockk<GameEngine>(relaxed = true)
    val audio = mockk<MegaAudioManager>(relaxed = true)
    val players = MegaPlayers(2)
    val gravity = WorldGravity(players)
    val gameState = GameState().also { it.setLives(lives) }
    val services = testServices(engine = engine, audio = audio, players = players, gravity = gravity, gameState = gameState)
    val camera = OrthographicCamera().also {
        it.viewportWidth = 100f
        it.viewportHeight = 80f
    }
    val view = CameraView(camera, CameraRotation())
    val center = Vector3(200f, 40f, 0f)
    val world = SimpleGridWorldContainer(32)
    val beams = Array(2) { mockk<BeamDown>(relaxed = true) }
    var hold = false
    val respawn = SlotRespawn(services, PartnerSpot(WarpSpotTest(32f, services.rules) { world }, view, center), beams) {
        hold
    }
    val first = player(0, 200f, 40f)
    val second = player(1, 230f, 40f)

    init {
        val floor = Body(BodyType.STATIC, 0f, 25f, 1000f, 10f)
        floor.putProperty(ConstKeys.ENTITY, TestFreezer(services))
        world.addBody(floor)
    }

    private fun player(slot: Int, x: Float, y: Float): TestHealthPlayer {
        val player = TestHealthPlayer(services)
        player.body.setSize(10f, 10f).setCenter(x, y)
        player.body.setBodySense(BodySense.FEET_ON_GROUND, true)
        players.register(PlayerSlot(slot), player)
        player.existenceState = EntityExistenceState.LIVE
        return player
    }

    fun kill(player: TestHealthPlayer, routineRunning: Boolean = false, defeatRunning: Boolean = false): Boolean {
        player.existenceState = EntityExistenceState.UNSPAWNED
        return respawn.onDeath(player, routineRunning, defeatRunning)
    }

    fun spawned(player: TestHealthPlayer): Boolean {
        repeatUpdate(11)
        return spawnProps(player) != null
    }

    fun spawnProps(player: TestHealthPlayer): Properties? {
        val props = slot<Properties>()
        verify(atMost = 1) { engine.spawn(player, capture(props)) }
        return if (props.isCaptured) props.captured else null
    }

    fun repeatUpdate(times: Int, delta: Float = 0.1f) = repeat(times) { respawn.update(delta) }
}

class SlotRespawnTest : DescribeSpec({

    describe("the death of a player") {
        it("respawns the player when the partner lives, and takes one life") {
            val rig = RespawnRig()
            rig.kill(rig.second) shouldBe false
            rig.gameState.getLives(PlayerSlot(1)) shouldBe 2
            rig.second.playPhase shouldBe PlayPhase.PRE_PLAY
            rig.spawned(rig.second) shouldBe true
        }

        it("works the same for slot 0, with slot 1 alive") {
            val rig = RespawnRig()
            rig.kill(rig.first) shouldBe false
            rig.spawned(rig.first) shouldBe true
        }

        it("starts the routine when no other player lives, and cancels a waiting respawn") {
            val rig = RespawnRig()
            rig.kill(rig.second)
            rig.kill(rig.first) shouldBe true
            rig.first.playPhase shouldBe PlayPhase.POST_PLAY
            rig.repeatUpdate(20)
            verify(exactly = 0) { rig.engine.spawn(any(), any()) }
        }

        it("while the routine runs, starts the routine again only for slot 0, as before") {
            val rig = RespawnRig()
            rig.kill(rig.second, routineRunning = true) shouldBe false
            rig.kill(rig.first, routineRunning = true) shouldBe true
            rig.repeatUpdate(20)
            verify(exactly = 0) { rig.engine.spawn(any(), any()) }
        }

        it("keeps a player out when the death takes the last life, while the partner plays on") {
            val rig = RespawnRig(lives = 1)
            rig.kill(rig.second) shouldBe false
            rig.second.playPhase shouldBe PlayPhase.POST_PLAY
            rig.repeatUpdate(50)
            verify(exactly = 0) { rig.engine.spawn(any(), any()) }
        }

        it("starts nothing while the defeat runs, so two deaths in one frame take one defeat") {
            val rig = RespawnRig()
            rig.kill(rig.second, routineRunning = true, defeatRunning = true) shouldBe false
            rig.kill(rig.first, routineRunning = true, defeatRunning = true) shouldBe false
            rig.first.playPhase shouldBe PlayPhase.POST_PLAY
            rig.second.playPhase shouldBe PlayPhase.POST_PLAY
        }

        it("counts a partner that beams in as present, so a death then waits for it and starts no routine") {
            val rig = RespawnRig()
            rig.kill(rig.first)
            rig.repeatUpdate(11)
            rig.first.existenceState = EntityExistenceState.LIVE
            rig.repeatUpdate(1)
            rig.first.playPhase shouldBe PlayPhase.PRE_PLAY

            rig.kill(rig.second) shouldBe false
            rig.repeatUpdate(20)
            verify(exactly = 0) { rig.engine.spawn(rig.second, any()) }

            every { rig.beams[0].finished } returns true
            rig.repeatUpdate(1)
            rig.first.playPhase shouldBe PlayPhase.IN_PLAY
            rig.repeatUpdate(1)
            verify(exactly = 1) { rig.engine.spawn(rig.second, any()) }
        }
    }

    describe("the respawn") {
        it("waits while the hold is on (a room transition or a gate), and the delay goes on after it") {
            val rig = RespawnRig()
            rig.kill(rig.second)
            rig.hold = true
            rig.repeatUpdate(30)
            verify(exactly = 0) { rig.engine.spawn(any(), any()) }

            rig.hold = false
            rig.repeatUpdate(9)
            verify(exactly = 0) { rig.engine.spawn(any(), any()) }
            rig.repeatUpdate(2)
            verify(exactly = 1) { rig.engine.spawn(rig.second, any()) }
        }

        it("waits 1 s, then spawns the player next to the partner, on the side the partner faces") {
            val rig = RespawnRig()
            rig.kill(rig.second)
            rig.repeatUpdate(9)
            verify(exactly = 0) { rig.engine.spawn(any(), any()) }

            rig.repeatUpdate(2)
            val props = rig.spawnProps(rig.second)!!
            props.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter(com.badlogic.gdx.math.Vector2()) shouldBe
                com.badlogic.gdx.math.Vector2(232f, 40f)
            props.get(ConstKeys.FACING, String::class) shouldBe "right"
            props.get(ConstKeys.DIRECTION, String::class) shouldBe "up"
        }

        it("uses the partner's facing and the current gravity direction, so a spawn never turns the gravity") {
            val rig = RespawnRig()
            rig.first.facing = Facing.LEFT
            rig.gravity.set(Direction.DOWN)
            rig.kill(rig.second)
            rig.repeatUpdate(11)
            val props = rig.spawnProps(rig.second)!!
            props.get(ConstKeys.FACING, String::class) shouldBe "left"
            props.get(ConstKeys.DIRECTION, String::class) shouldBe "down"
        }

        it("waits while the partner is in the air, and spawns when it stands") {
            val rig = RespawnRig()
            rig.first.body.setBodySense(BodySense.FEET_ON_GROUND, false)
            rig.kill(rig.second)
            rig.repeatUpdate(20)
            verify(exactly = 0) { rig.engine.spawn(any(), any()) }

            rig.first.body.setBodySense(BodySense.FEET_ON_GROUND, true)
            rig.repeatUpdate(1)
            verify(exactly = 1) { rig.engine.spawn(rig.second, any()) }
        }

        it("lands on the partner's spot after the cap while no side spot is safe") {
            val rig = RespawnRig()
            rig.first.body.setBodySense(BodySense.FEET_ON_GROUND, false)
            rig.kill(rig.second)
            rig.repeatUpdate(39)
            verify(exactly = 0) { rig.engine.spawn(any(), any()) }

            rig.repeatUpdate(2)
            rig.spawnProps(rig.second)!!.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter(com.badlogic.gdx.math.Vector2()) shouldBe
                com.badlogic.gdx.math.Vector2(200f, 40f)
        }

        it("beams in when the spawn is done, then puts the player in play") {
            val rig = RespawnRig()
            every { rig.beams[1].finished } returns false
            rig.kill(rig.second)
            rig.repeatUpdate(11)
            rig.second.existenceState = EntityExistenceState.LIVE
            rig.repeatUpdate(1)

            verify(exactly = 1) { rig.beams[1].start(rig.second) }
            rig.second.isLocked(MegaLockGroup.DAMAGE) shouldBe true
            rig.second.isLocked(MegaLockGroup.BEHAVIORS) shouldBe true
            rig.second.isLocked(MegaLockGroup.MOVEMENT) shouldBe true
            rig.second.body.physics.gravityOn shouldBe false
            rig.second.playPhase shouldBe PlayPhase.PRE_PLAY

            every { rig.beams[1].finished } returns true
            rig.repeatUpdate(1)
            rig.second.isLocked(MegaLockGroup.DAMAGE) shouldBe false
            rig.second.isLocked(MegaLockGroup.BEHAVIORS) shouldBe false
            rig.second.isLocked(MegaLockGroup.MOVEMENT) shouldBe false
            rig.second.body.physics.gravityOn shouldBe true
            rig.second.playPhase shouldBe PlayPhase.IN_PLAY
            verify(exactly = 1) { rig.audio.playSound(SoundAsset.BEAM_SOUND) }
        }

        it("stops when the level resets") {
            val rig = RespawnRig()
            rig.kill(rig.second)
            rig.respawn.reset()
            rig.repeatUpdate(20)
            verify(exactly = 0) { rig.engine.spawn(any(), any()) }
            verify { rig.beams[1].stop() }
        }
    }
})
