package com.megaman.maverick.game.fights

import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.state.Step
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.screens.levels.stats.BossHealthHandler
import com.megaman.maverick.game.testServices
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk

private class Probe(val name: String, val log: ArrayList<String>, var done: Boolean = true) : Step<Fight> {

    override fun begin(context: Fight) {
        log.add("begin:$name")
    }

    override fun isFinished(context: Fight) = done

    override fun end(context: Fight, interrupted: Boolean) {
        log.add("end:$name:$interrupted")
    }
}

private fun plan(intro: Array<Step<Fight>>, outro: Array<Step<Fight>> = Array()) =
    FightPlan(intro, FightRule.AllDefeated, outro, MusicAsset.VINNYZ_INTRO_BOSS_MUSIC_INTRO)

private fun boss(defeated: () -> Boolean = { false }) = mockk<AbstractBoss>(relaxed = true).also {
    every { it.defeated } answers { defeated() }
}

private fun fight(boss: AbstractBoss, plan: FightPlan) =
    Fight(boss, plan, testServices(), mockk<BossHealthHandler>(relaxed = true))

class FightTest : DescribeSpec({

    describe("Fight") {
        it("runs the intro, waits for the win rule, runs the outro and closes") {
            val log = ArrayList<String>()
            var defeated = false
            val fight = fight(
                boss { defeated },
                plan(gdxArrayOf(Probe("intro", log)), gdxArrayOf(Probe("outro", log)))
            )

            fight.update()
            fight.update()
            log.shouldContainExactly("begin:intro", "end:intro:false")
            fight.closed shouldBe false

            defeated = true
            fight.update()
            log.shouldContainExactly("begin:intro", "end:intro:false", "begin:outro", "end:outro:false")
            fight.closed shouldBe true
        }

        it("releases each lease once, in reverse order, when it closes") {
            val log = ArrayList<String>()
            val fight = fight(boss { true }, plan(Array()))
            fight.lease(FightLease.WORLD_HOLD) { log.add("world") }
            fight.lease(FightLease.HEALTH_BAR) { log.add("bar") }

            fight.update()
            fight.cancel()

            log.shouldContainExactly("bar", "world")
        }

        it("releases one lease by its key, and only once") {
            val log = ArrayList<String>()
            val fight = fight(boss(), plan(Array()))
            fight.lease(FightLease.WORLD_HOLD) { log.add("world") }

            fight.release(FightLease.WORLD_HOLD)
            fight.release(FightLease.WORLD_HOLD)
            fight.cancel()

            log.shouldContainExactly("world")
        }

        it("cancels the intro and goes to the outro on a win during the intro") {
            val log = ArrayList<String>()
            var defeated = false
            val fight = fight(
                boss { defeated },
                plan(gdxArrayOf(Probe("intro", log, done = false)), gdxArrayOf(Probe("outro", log)))
            )
            fight.lease(FightLease.WORLD_HOLD) { log.add("world") }

            fight.update()
            defeated = true
            fight.update()

            log.shouldContainExactly("begin:intro", "end:intro:true", "world", "begin:outro", "end:outro:false")
            fight.closed shouldBe true
        }

        it("ends a running step as interrupted on a cancel") {
            val log = ArrayList<String>()
            val fight = fight(boss(), plan(gdxArrayOf(Probe("intro", log, done = false))))

            fight.update()
            fight.cancel()

            log.shouldContainExactly("begin:intro", "end:intro:true")
            fight.closed shouldBe true
        }
    }

    describe("FightDirector") {
        val bar = mockk<BossHealthHandler>(relaxed = true)

        it("does nothing with no fight") {
            val director = FightDirector()

            director.update()
            director.cancel()

            director.active shouldBe false
        }

        it("owns only the boss of its fight") {
            val director = FightDirector()
            val boss = boss()

            director.start(boss, plan(gdxArrayOf(Probe("wait", ArrayList(), done = false))), testServices(), bar)

            director.active shouldBe true
            director.owns(boss) shouldBe true
            director.owns(boss()) shouldBe false
        }

        it("drops a closed fight") {
            val director = FightDirector()

            director.start(boss { true }, plan(Array()), testServices(), bar)
            director.update()

            director.active shouldBe false
        }

        it("drops a cancelled fight") {
            val director = FightDirector()

            director.start(boss(), plan(Array()), testServices(), bar)
            director.cancel()

            director.active shouldBe false
        }
    }

    describe("FightPlans") {
        it("has a plan for BigAssMaverickRobot") {
            FightPlans.find("BigAssMaverickRobot") shouldNotBe null
        }

        it("has no plan for any other boss") {
            listOf(
                "Bospider", "DesertMan", "ElecDevil", "GlacierMan", "GutsTank", "InfernoMan", "MechaDragon",
                "MoonHead", "MoonMan", "PenguinMiniBoss", "PreciousWoman", "ReactorMan", "ReactorMonkey",
                "RodentMan", "SigmaRat", "TimberWoman", "WilyFinalBoss"
            ).forEach { FightPlans.find(it) shouldBe null }
        }

        it("writes the music of the plan to the spawn props") {
            val spawnProps = Properties()

            FightPlans.resolve(FightPlans.find("BigAssMaverickRobot")!!, spawnProps)

            spawnProps.get(ConstKeys.MUSIC) shouldBe MusicAsset.VINNYZ_INTRO_BOSS_MUSIC_INTRO.name
        }
    }
})
