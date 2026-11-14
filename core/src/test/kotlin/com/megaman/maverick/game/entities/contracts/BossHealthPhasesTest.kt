package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.drawables.ui.BossBitsBar
import com.megaman.maverick.game.entities.bosses.BOSS_RIG_PPM
import com.megaman.maverick.game.entities.bosses.BossRig
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.BodyComponentCreator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

private class PhasedBoss(services: MegaServices, override val healthPhases: Int) : AbstractBoss(services) {

    override val healthBarRegions: Array<String> =
        if (healthPhases == 2) gdxArrayOf(ConstKeys.STANDARD, BossBitsBar.PHASE_2_BITS) else gdxArrayOf(ConstKeys.STANDARD)

    var damage = 1
    val phaseStarts = Array<Int>()

    override val damageNegotiator = object : IDamageNegotiator {
        override fun get(damager: IDamagerEntity) = damage
    }

    override fun onHealthPhaseStart(phase: Int) {
        phaseStarts.add(phase)
    }

    override fun isReady(delta: Float) = true

    override fun defineBodyComponent() =
        BodyComponentCreator.create(this, Body(BodyType.ABSTRACT).also { it.setSize(BOSS_RIG_PPM) })

    override fun defineSpritesComponent() = SpritesComponent()
}

private class BadPhasedBoss(services: MegaServices) : AbstractBoss(services) {

    override val healthPhases = 2

    override fun isReady(delta: Float) = true

    override fun defineBodyComponent() =
        BodyComponentCreator.create(this, Body(BodyType.ABSTRACT).also { it.setSize(BOSS_RIG_PPM) })

    override fun defineSpritesComponent() = SpritesComponent()
}

class BossHealthPhasesTest : DescribeSpec({

    lateinit var rig: BossRig

    beforeEach { rig = BossRig() }

    afterEach { rig.dispose() }

    fun spawn(phases: Int): PhasedBoss {
        val boss = PhasedBoss(rig.services, phases)
        boss.spawn(props()) shouldBe true
        rig.processSpawns()
        return boss
    }

    fun PhasedBoss.hit(amount: Int) {
        damage = amount
        takeDamageFrom(rig.bullet()) shouldBe true
        rig.checkPoints(this)
        rig.runEvents()
    }

    describe("a boss with 2 health phases") {
        it("rolls over to phase 2 with full health after exactly 30 damage, with no defeat") {
            val boss = spawn(2)
            boss.hit(30)
            boss.healthPhase shouldBe 2
            boss.getCurrentHealth() shouldBe 30
            boss.defeated shouldBe false
            rig.eventsOf(EventType.BOSS_DEFEATED) shouldBe 0
        }

        it("carries the extra damage into the next phase") {
            val boss = spawn(2)
            boss.hit(28)
            boss.getCurrentHealth() shouldBe 2
            boss.hit(3)
            boss.healthPhase shouldBe 2
            boss.getCurrentHealth() shouldBe 29
        }

        it("calls onHealthPhaseStart once for the new phase") {
            val boss = spawn(2)
            boss.hit(10)
            boss.phaseStarts.size shouldBe 0
            boss.hit(25)
            boss.hit(2)
            boss.phaseStarts shouldBe gdxArrayOf(2)
        }

        it("is defeated once at 0 in the last phase") {
            val boss = spawn(2)
            boss.hit(30)
            boss.hit(30)
            boss.getCurrentHealth() shouldBe 0
            boss.defeated shouldBe true
            rig.checkPoints(boss)
            rig.runEvents()
            rig.eventsOf(EventType.BOSS_DEFEATED) shouldBe 1
            boss.phaseStarts shouldBe gdxArrayOf(2)
        }

        it("keeps at least 1 health in the next phase when one hit is larger than the rest of the phase and a full bar") {
            listOf(60, 75).forEach { amount ->
                val boss = spawn(2)
                boss.hit(amount)
                withClue("hit $amount") {
                    boss.healthPhase shouldBe 2
                    boss.getCurrentHealth() shouldBe 1
                    boss.defeated shouldBe false
                    rig.eventsOf(EventType.BOSS_DEFEATED) shouldBe 0
                    boss.phaseStarts shouldBe gdxArrayOf(2)
                }
            }
        }

        it("gives the ratio over the whole fight") {
            val boss = spawn(2)
            boss.getHealthRatio() shouldBe (1f plusOrMinus 0.0001f)
            boss.hit(15)
            boss.getHealthRatio() shouldBe (0.75f plusOrMinus 0.0001f)
            boss.hit(15)
            boss.getHealthRatio() shouldBe (0.5f plusOrMinus 0.0001f)
            boss.hit(15)
            boss.getHealthRatio() shouldBe (0.25f plusOrMinus 0.0001f)
            boss.hit(15)
            boss.getHealthRatio() shouldBe (0f plusOrMinus 0.0001f)
        }

        it("starts in phase 1 with full health when it spawns again after a level reset") {
            val boss = spawn(2)
            boss.hit(33)
            boss.healthPhase shouldBe 2
            rig.services.events.submitEvent(Event(EventType.LEVEL_RESET, Properties()))
            rig.runEvents()
            rig.destroyed.contains(boss, true) shouldBe true
            boss.onDestroy()
            boss.spawn(props()) shouldBe true
            rig.processSpawns()
            boss.healthPhase shouldBe 1
            boss.getCurrentHealth() shouldBe 30
            boss.getHealthRatio() shouldBe (1f plusOrMinus 0.0001f)
        }
    }

    describe("a boss with 1 health phase") {
        it("is defeated at 0 as before, with the plain ratio") {
            val boss = spawn(1)
            boss.hit(15)
            boss.getHealthRatio() shouldBe (0.5f plusOrMinus 0.0001f)
            boss.hit(20)
            boss.healthPhase shouldBe 1
            boss.getCurrentHealth() shouldBe 0
            boss.defeated shouldBe true
            boss.phaseStarts.size shouldBe 0
            rig.eventsOf(EventType.BOSS_DEFEATED) shouldBe 1
        }
    }

    describe("the bar regions") {
        it("fail fast when they do not match the phases") {
            shouldThrow<IllegalStateException> { BadPhasedBoss(rig.services).init() }
        }
    }
})
