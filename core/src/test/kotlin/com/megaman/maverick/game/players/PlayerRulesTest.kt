package com.megaman.maverick.game.players

import com.maverick.game.engine.components.MissingComponentException
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.players.IPlayer
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.difficulty.DifficultyMode
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.bosses.BigAssMaverickRobot
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.state.DamageRecoveryComponent
import com.megaman.maverick.game.entities.state.PlayPhase
import com.megaman.maverick.game.entities.state.StunComponent
import com.megaman.maverick.game.entities.state.TagsComponent
import com.megaman.maverick.game.entities.state.setPlayPhase
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.state.GameMode
import com.megaman.maverick.game.state.SessionRules
import com.megaman.maverick.game.testServices
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll

class PlayerRulesTest : DescribeSpec({

    lateinit var player: TestHealthPlayer

    fun session(rules: SessionRules = GameMode.SINGLE.rules): Pair<TestHealthPlayer, TestHealthPlayer> {
        val slots = MegaPlayers(2)
        val services = testServices(players = slots, rules = rules)
        val first = TestHealthPlayer(services)
        val second = TestHealthPlayer(services)
        slots.register(PlayerSlot(0), first)
        slots.register(PlayerSlot(1), second)
        first.existenceState = EntityExistenceState.LIVE
        second.existenceState = EntityExistenceState.LIVE
        return first to second
    }

    beforeEach { player = session().first }

    afterEach { unmockkAll() }

    describe("invincible") {
        it("is false for a live player in play") {
            player.invincible shouldBe false
        }

        it("is true while damaged") {
            player.startDamage()
            player.invincible shouldBe true
        }

        it("is true during recovery") {
            player.recovery.update(0f, damaged = false, damageJustFinished = true)
            player.invincible shouldBe true
        }

        it("is true with a damage lock") {
            player.lock(MegaLockGroup.DAMAGE, MegaLockReason.TELEPORT)
            player.invincible shouldBe true
        }

        it("is true when the player does not stay in the engine") {
            player.existenceState = EntityExistenceState.UNSPAWNED
            player.invincible shouldBe true
        }

        it("is true before play and after play") {
            player.setPlayPhase(PlayPhase.PRE_PLAY)
            player.invincible shouldBe true

            player.setPlayPhase(PlayPhase.POST_PLAY)
            player.invincible shouldBe true
        }

        it("is false while stunned or frozen") {
            player.stun.start()
            player.freeze.freeze()
            player.invincible shouldBe false
        }

        it("throws when a component that it reads is missing") {
            player.removeComponent(DamageRecoveryComponent::class)
            shouldThrow<MissingComponentException> { player.invincible }
        }
    }

    describe("playerCanMove") {
        it("is true with no block") {
            player.playerCanMove shouldBe true
        }

        it("is false with a movement lock") {
            player.lock(MegaLockGroup.MOVEMENT, MegaLockReason.CAMERA_ROTATION)
            player.playerCanMove shouldBe false
        }

        it("is false while stunned") {
            player.stun.start()
            player.playerCanMove shouldBe false
        }

        it("is false while damaged") {
            player.startDamage()
            player.playerCanMove shouldBe false
        }

        it("is false while frozen") {
            player.freeze.freeze()
            player.playerCanMove shouldBe false
        }

        it("throws when a component that it reads is missing") {
            player.removeComponent(StunComponent::class)
            shouldThrow<MissingComponentException> { player.playerCanMove }
        }
    }

    describe("playerAllowsDamageFrom") {
        it("refuses the player's own shot") {
            player.playerAllowsDamageFrom(TestOwnedDamager(player.services, player), player.services.rules) shouldBe false
        }

        it("allows a damager that no player owns") {
            player.playerAllowsDamageFrom(TestOwnedDamager(player.services, null), player.services.rules) shouldBe true
        }

        it("follows friendly fire for a teammate's shot") {
            val (coop, coopMate) = session(GameMode.COOP.rules)
            coop.playerAllowsDamageFrom(TestOwnedDamager(coop.services, coopMate), coop.services.rules) shouldBe false

            val (pvp, pvpMate) = session(GameMode.PVP.rules)
            pvp.playerAllowsDamageFrom(TestOwnedDamager(pvp.services, pvpMate), pvp.services.rules) shouldBe true
        }

        it("refuses all damage when the player does not stay in the engine") {
            player.existenceState = EntityExistenceState.UNSPAWNED
            player.playerAllowsDamageFrom(TestOwnedDamager(player.services, null), player.services.rules) shouldBe false
        }
    }

    describe("the damage tail in takeDamageFrom") {
        it("sends the damage signal with the player and the damager") {
            var signalled: Pair<IPlayer?, IDamagerEntity?>? = null
            player.players.playerDamaged.connect { signalled = it.player to it.damager }
            val damager = TestOwnedDamager(player.services, null)

            player.takeDamageFrom(damager) shouldBe true

            signalled shouldBe (player to damager)
        }

        it("freezes the player when the damager is a freezer") {
            player.takeDamageFrom(TestFreezingExplosion(player.services)) shouldBe true
            player.freeze.frozen shouldBe true
        }
    }

    describe("the damage edit in takeDamageFrom") {
        fun damageTaken(): Int {
            val before = player.getCurrentHealth()
            player.takeDamageFrom(TestOwnedDamager(player.services, null)) shouldBe true
            return before - player.getCurrentHealth()
        }

        it("keeps the base damage in normal mode with no enhancement") {
            damageTaken() shouldBe 3
        }

        it("adds 1 in hard mode") {
            player.services.gameState.setDifficultyMode(DifficultyMode.HARD)
            damageTaken() shouldBe 4
        }

        it("scales the damage with the damage-increase enhancement") {
            player.services.gameState.addEnhancement(MegaEnhancement.DAMAGE_INCREASE)
            val scaled = MegaEnhancement.scaleDamage(3, MegaEnhancement.MEGAMAN_DAMAGE_INCREASE_SCALAR)
            damageTaken() shouldBe scaled
        }
    }

    describe("PlayerDamageNegotiator") {
        fun damager(tag: String, type: EntityType): IDamagerEntity {
            val entity = mockk<MegaGameEntity>(relaxed = true, moreInterfaces = arrayOf(IDamagerEntity::class))
            every { entity.getComponent(TagsComponent::class) } returns TagsComponent(tag)
            every { entity.getType() } returns type
            return entity as IDamagerEntity
        }

        it("gives 0 for the player's own shot") {
            PlayerDamageNegotiator(player).get(TestOwnedDamager(player.services, player)) shouldBe 0
        }

        it("gives the table value for a tag in the table") {
            PlayerDamageNegotiator(player).get(damager(BigAssMaverickRobot.TAG, EntityType.BOSS)) shouldBe 1
        }

        it("gives 3 for an enemy or an explosion that is not in the table") {
            PlayerDamageNegotiator(player).get(damager("Other", EntityType.ENEMY)) shouldBe 3
            PlayerDamageNegotiator(player).get(TestOwnedDamager(player.services, null)) shouldBe 3
        }

        it("gives 0 for another type") {
            PlayerDamageNegotiator(player).get(damager("Other", EntityType.BLOCK)) shouldBe 0
        }
    }
})

private class TestFreezingExplosion(services: MegaServices) : MegaGameEntity(services), IDamagerEntity, IFreezerEntity {

    init {
        init()
    }

    override fun getType() = EntityType.EXPLOSION
}
