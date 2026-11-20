package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.glacierman.GlacierManRig
import com.megaman.maverick.game.entities.bosses.glacierman.STEP
import com.megaman.maverick.game.entities.bosses.glacierman.T
import com.megaman.maverick.game.entities.projectiles.FallingIcicle.FallingIcicleState
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

private const val TOLERANCE = 2f * STEP
private const val ICICLE_X = 20f

class FallingIcicleTest : DescribeSpec({

    lateinit var rig: GlacierManRig

    beforeEach { rig = GlacierManRig() }

    afterEach { rig.dispose() }

    fun spawn(extra: Properties = Properties(), icicle: FallingIcicle? = null): FallingIcicle {
        val out = icicle ?: MegaEntityFactory.fetch(FallingIcicle::class)!!
        val spawnProps = props(ConstKeys.POSITION pairTo Vector2(ICICLE_X * T, 10f * T))
        spawnProps.putAll(extra)
        out.spawn(spawnProps) shouldBe true
        rig.processSpawns()
        return out
    }

    fun animator(icicle: FallingIcicle): Animator {
        var found: Animator? = null
        icicle.getComponent(AnimationsComponent::class)!!.forEachAnimator { _, _, animator -> found = animator as Animator }
        return found!!
    }

    fun key(icicle: FallingIcicle): String? {
        val animator = animator(icicle)
        return animator.keySupplier(animator.currentKey)
    }

    fun step(icicle: FallingIcicle) =
        icicle.getComponent(UpdatablesComponent::class)!!.updatables.values().forEach { it.update(STEP) }

    fun falling(icicle: FallingIcicle) = icicle.body.physics.gravityOn

    fun secondsWhile(icicle: FallingIcicle, max: Float = 5f, condition: () -> Boolean): Float {
        var count = 0
        while (condition() && count * STEP < max) {
            step(icicle)
            count++
        }
        return count * STEP
    }

    fun playerAway() = rig.place(6f)

    fun playerUnder() = rig.place(ICICLE_X)

    describe("the default path (no DELAY, no DURATION, no FORM), as on master") {
        it("a map icicle (BOUNDS) waits in STILL until a live player is under it, shakes 0.25 s, then falls") {
            playerAway()
            val icicle = MegaEntityFactory.fetch(FallingIcicle::class)!!
            icicle.spawn(props(ConstKeys.BOUNDS pairTo GameRectangle(19.5f * T, 9f * T, T, T))) shouldBe true
            rig.processSpawns()
            icicle.body.getMaxY() shouldBe (10f * T plusOrMinus 0.01f)
            repeat(300) { step(icicle) }
            key(icicle) shouldBe "still"
            falling(icicle) shouldBe false
            playerUnder()
            step(icicle)
            key(icicle) shouldBe "shake"
            val shake = secondsWhile(icicle) { key(icicle) == "shake" } + STEP
            shake shouldBe (0.25f plusOrMinus TOLERANCE)
            step(icicle)
            falling(icicle) shouldBe true
        }

        it("a POSITION spawn with no STATE waits for the player in the same way") {
            playerAway()
            val icicle = spawn()
            repeat(300) { step(icicle) }
            falling(icicle) shouldBe false
            key(icicle) shouldBe "still"
            playerUnder()
            step(icicle)
            key(icicle) shouldBe "shake"
        }

        it("a player over the icicle is not under it") {
            rig.placePlayerCenter(ICICLE_X * T, 12f * T)
            val icicle = spawn()
            repeat(60) { step(icicle) }
            key(icicle) shouldBe "still"
        }

        listOf<Any>(FallingIcicleState.FALL, "fall").forEach { state ->
            it("STATE = FALL (${state::class.simpleName}, the IcicleTelly icicle) falls at once") {
                playerAway()
                val icicle = spawn(props(ConstKeys.STATE pairTo state))
                falling(icicle) shouldBe false
                step(icicle)
                falling(icicle) shouldBe true
                key(icicle) shouldBe "still"
            }
        }
    }

    describe("the form strip") {
        it("STATE = FORM plays form (4 x 0.15 s, no loop), then goes to STILL") {
            playerAway()
            val icicle = spawn(props(ConstKeys.STATE pairTo FallingIcicleState.FORM))
            key(icicle) shouldBe "form"
            val form = animator(icicle).animations["form"]
            form.isLooping() shouldBe false
            form.getDuration() shouldBe (0.6f plusOrMinus 0.0001f)
            (form as Animation).size() shouldBe 4
            val seconds = secondsWhile(icicle) { key(icicle) == "form" }
            seconds shouldBe (0.6f plusOrMinus TOLERANCE)
            key(icicle) shouldBe "still"
            repeat(120) { step(icicle) }
            key(icicle) shouldBe "still"
            falling(icicle) shouldBe false
        }
    }

    describe("the timed drop") {
        it("with DELAY: STILL for that time, a shake for DURATION, then a fall with no player under it") {
            playerAway()
            val icicle = spawn(props(ConstKeys.DELAY pairTo 0.4f, ConstKeys.DURATION pairTo 0.3f))
            key(icicle) shouldBe "still"
            val still = secondsWhile(icicle) { key(icicle) == "still" }
            still shouldBe (0.4f plusOrMinus TOLERANCE)
            key(icicle) shouldBe "shake"
            val shake = secondsWhile(icicle) { key(icicle) == "shake" } + STEP
            shake shouldBe (0.3f plusOrMinus TOLERANCE)
            step(icicle)
            falling(icicle) shouldBe true
        }

        it("with DELAY and no DURATION, the shake is 0.25 s") {
            playerAway()
            val icicle = spawn(props(ConstKeys.DELAY pairTo 0.2f))
            secondsWhile(icicle) { key(icicle) == "still" }
            val shake = secondsWhile(icicle) { key(icicle) == "shake" } + STEP
            shake shouldBe (0.25f plusOrMinus TOLERANCE)
        }

        it("FORM, then DELAY, then DURATION, then the fall") {
            playerAway()
            val icicle = spawn(
                props(
                    ConstKeys.STATE pairTo FallingIcicleState.FORM,
                    ConstKeys.DELAY pairTo 0.35f,
                    ConstKeys.DURATION pairTo 0.35f
                )
            )
            secondsWhile(icicle) { key(icicle) == "form" } shouldBe (0.6f plusOrMinus TOLERANCE)
            secondsWhile(icicle) { key(icicle) == "still" } shouldBe (0.35f plusOrMinus TOLERANCE)
            (secondsWhile(icicle) { key(icicle) == "shake" } + STEP) shouldBe (0.35f plusOrMinus TOLERANCE)
            step(icicle)
            falling(icicle) shouldBe true
        }
    }

    describe("a pooled reuse") {
        it("a timed spawn, then a spawn with no props: the reuse waits for the player and shakes 0.25 s") {
            playerAway()
            val icicle = spawn(props(ConstKeys.DELAY pairTo 0.1f, ConstKeys.DURATION pairTo 0.5f))
            repeat(12) { step(icicle) }
            key(icicle) shouldBe "shake"
            icicle.destroy()
            rig.processDestroys()
            spawn(icicle = icicle) shouldBe icicle
            repeat(120) { step(icicle) }
            key(icicle) shouldBe "still"
            falling(icicle) shouldBe false
            playerUnder()
            step(icicle)
            (secondsWhile(icicle) { key(icicle) == "shake" } + STEP) shouldBe (0.25f plusOrMinus TOLERANCE)
        }

        it("a spawn that ends in the form, the still or the shake: the reuse runs each timer in full") {
            playerAway()
            val timed = props(
                ConstKeys.STATE pairTo FallingIcicleState.FORM,
                ConstKeys.DELAY pairTo 0.3f,
                ConstKeys.DURATION pairTo 0.3f
            )
            val icicle = MegaEntityFactory.fetch(FallingIcicle::class)!!
            listOf(30 to "form", 50 to "still", 63 to "shake").forEach { (ticks, at) ->
                withClue("destroyed after $ticks ticks") {
                    spawn(timed, icicle)
                    repeat(ticks) { step(icicle) }
                    key(icicle) shouldBe at
                    icicle.destroy()
                    rig.processDestroys()
                    spawn(timed, icicle)
                    secondsWhile(icicle) { key(icicle) == "form" } shouldBe (0.6f plusOrMinus TOLERANCE)
                    secondsWhile(icicle) { key(icicle) == "still" } shouldBe (0.3f plusOrMinus TOLERANCE)
                    (secondsWhile(icicle) { key(icicle) == "shake" } + STEP) shouldBe (0.3f plusOrMinus TOLERANCE)
                    icicle.destroy()
                    rig.processDestroys()
                }
            }
        }
    }
})
