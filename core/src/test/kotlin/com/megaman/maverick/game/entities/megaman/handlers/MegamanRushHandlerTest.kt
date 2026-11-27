package com.megaman.maverick.game.entities.megaman.handlers

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.enums.Direction
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.weapons.MegamanWeapons
import com.megaman.maverick.game.weapons.WeaponDef
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

private class FakeRush(private val spawns: Boolean = true) : RushHandle {

    val calls = Array<String>()

    override fun arrive(owner: Megaman, weapon: WeaponDef<Megaman>): Boolean {
        calls.add("arrive:${weapon.id}")
        return spawns
    }

    override fun leave() {
        calls.add("leave")
    }

    override fun leaveFromBack(owner: Megaman): Boolean {
        calls.add("leaveFromBack")
        return spawns
    }

    override fun vanish() {
        calls.add("vanish")
    }
}

class MegamanRushHandlerTest : DescribeSpec({

    lateinit var megaman: Megaman
    lateinit var weaponsHandler: MegamanWeaponsHandler
    lateinit var rushes: Array<FakeRush>
    var rushesSpawn = true
    lateinit var handler: MegamanRushHandler

    beforeEach {
        weaponsHandler = mockk(relaxed = true)
        every { weaponsHandler.isDepleted(any()) } returns false
        megaman = mockk(relaxed = true)
        every { megaman.weaponsHandler } returns weaponsHandler
        every { megaman.direction } returns Direction.UP
        rushes = Array()
        rushesSpawn = true
        handler = MegamanRushHandler(megaman) { FakeRush(rushesSpawn).also { rushes.add(it) } }
    }

    fun joinFirstRush() {
        handler.summon(MegamanWeapons.RUSH_JET) shouldBe true
        handler.onJoinStarted(rushes[0]) shouldBe true
        handler.onJoined(rushes[0])
    }

    describe("summon") {
        it("calls Rush for a Rush ability when he is absent") {
            handler.summon(MegamanWeapons.RUSH_JET) shouldBe true
            handler.phase shouldBe RushPhase.ARRIVING
            rushes.single().calls.toList() shouldBe listOf("arrive:rush_jet")
        }

        it("does not call Rush for a weapon that is not a Rush ability") {
            handler.summon(MegamanWeapons.MEGA_BUSTER) shouldBe false
            handler.phase shouldBe RushPhase.ABSENT
            rushes.size shouldBe 0
        }

        it("does not call Rush when the ability is depleted") {
            every { weaponsHandler.isDepleted(MegamanWeapons.RUSH_JET) } returns true
            handler.summon(MegamanWeapons.RUSH_JET) shouldBe false
            rushes.size shouldBe 0
        }

        it("does not call Rush when Megaman is not upright") {
            every { megaman.direction } returns Direction.LEFT
            handler.summon(MegamanWeapons.RUSH_JET) shouldBe false
            rushes.size shouldBe 0
        }

        it("goes back to absent when Rush does not spawn") {
            rushesSpawn = false
            handler.summon(MegamanWeapons.RUSH_JET) shouldBe false
            handler.phase shouldBe RushPhase.ABSENT
        }

        it("does not call a second Rush while one is out, joined or leaving") {
            handler.summon(MegamanWeapons.RUSH_JET) shouldBe true
            handler.summon(MegamanWeapons.RUSH_JET) shouldBe false
            handler.onJoinStarted(rushes[0]) shouldBe true
            handler.summon(MegamanWeapons.RUSH_JET) shouldBe false
            handler.onJoined(rushes[0])
            handler.summon(MegamanWeapons.RUSH_JET) shouldBe false
            handler.dismiss()
            handler.summon(MegamanWeapons.RUSH_JET) shouldBe false
            rushes.size shouldBe 2
        }
    }

    describe("join") {
        it("is joined only after the join ends") {
            handler.summon(MegamanWeapons.RUSH_JET)
            handler.joined shouldBe false
            handler.onJoinStarted(rushes[0])
            handler.phase shouldBe RushPhase.JOINING
            handler.joined shouldBe false
            handler.onJoined(rushes[0])
            handler.joined shouldBe true
        }

        it("stays joined when the joined Rush entity goes away") {
            joinFirstRush()
            handler.onRushGone(rushes[0])
            handler.phase shouldBe RushPhase.JOINED
        }

        it("ignores a Rush that is not the current one") {
            handler.summon(MegamanWeapons.RUSH_JET)
            handler.onJoinStarted(FakeRush()) shouldBe false
            handler.phase shouldBe RushPhase.ARRIVING
        }
    }

    describe("dismiss") {
        it("sends an arriving Rush away where he is") {
            handler.summon(MegamanWeapons.RUSH_JET)
            handler.dismiss()
            handler.phase shouldBe RushPhase.LEAVING
            rushes.single().calls.toList() shouldBe listOf("arrive:rush_jet", "leave")
        }

        it("sends a joining Rush away") {
            handler.summon(MegamanWeapons.RUSH_JET)
            handler.onJoinStarted(rushes[0])
            handler.dismiss()
            handler.phase shouldBe RushPhase.LEAVING
            rushes.single().calls.toList() shouldBe listOf("arrive:rush_jet", "leave")
        }

        it("beams a joined Rush out from Megaman's back") {
            joinFirstRush()
            handler.dismiss()
            handler.phase shouldBe RushPhase.LEAVING
            handler.joined shouldBe false
            rushes[1].calls.toList() shouldBe listOf("leaveFromBack")
        }

        it("goes back to absent when the Rush on the back does not spawn") {
            joinFirstRush()
            rushesSpawn = false
            handler.dismiss()
            handler.phase shouldBe RushPhase.ABSENT
        }

        it("lets Megaman call Rush again only after the leaving Rush is gone") {
            joinFirstRush()
            handler.dismiss()
            handler.onRushGone(rushes[1])
            handler.phase shouldBe RushPhase.ABSENT
            handler.summon(MegamanWeapons.RUSH_JET) shouldBe true
        }

        it("does nothing when Rush is absent") {
            handler.dismiss()
            handler.phase shouldBe RushPhase.ABSENT
            rushes.size shouldBe 0
        }
    }

    describe("gone and reset") {
        it("lets Megaman call again after an arriving Rush leaves the view") {
            handler.summon(MegamanWeapons.RUSH_JET)
            handler.onRushGone(rushes[0])
            handler.phase shouldBe RushPhase.ABSENT
            handler.summon(MegamanWeapons.RUSH_JET) shouldBe true
        }

        it("vanishes a live Rush and ignores its later destroy") {
            handler.summon(MegamanWeapons.RUSH_JET)
            handler.reset()
            handler.phase shouldBe RushPhase.ABSENT
            rushes[0].calls.toList() shouldBe listOf("arrive:rush_jet", "vanish")
            handler.summon(MegamanWeapons.RUSH_JET) shouldBe true
            handler.onRushGone(rushes[0])
            handler.phase shouldBe RushPhase.ARRIVING
        }

        it("forgets a joined Rush") {
            joinFirstRush()
            handler.reset()
            handler.joined shouldBe false
            handler.phase shouldBe RushPhase.ABSENT
        }
    }
})
