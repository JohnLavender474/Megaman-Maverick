package com.megaman.maverick.game.entities.bosses.elecdevil

import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.bosses.ElecDevil
import com.megaman.maverick.game.entities.bosses.ElecDevilBody
import com.megaman.maverick.game.entities.bosses.ElecDevilConstants
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import java.io.File

private const val ATLAS = "../assets/sprites/sprite_sheets/Bosses4.txt"
private const val ART_SCALE = 2
private const val FRAMES = 8
private const val TIME_TOLERANCE = 0.0001f

private fun atlasRegionSize(name: String): Pair<Int, Int> {
    val lines = File(ATLAS).readLines()
    val index = lines.indexOfFirst { it.trim() == name }
    if (index < 0) throw IllegalStateException("$ATLAS has no region $name")
    val size = lines.drop(index + 1).first { it.trim().startsWith("size:") }
        .substringAfter("size:").split(",").map { it.trim().toInt() }
    return size[0] to size[1]
}

class ElecDevilArtTest : DescribeSpec({

    val frameWidth = (ElecDevilConstants.BODY_SPRITE_WIDTH * ConstVals.PPM).toInt() / ART_SCALE
    val frameHeight = (ElecDevilConstants.BODY_SPRITE_HEIGHT * ConstVals.PPM).toInt() / ART_SCALE

    describe("the Bosses4 atlas") {
        listOf("appear", "defeated").forEach { name ->
            it("holds $FRAMES frames of the body size in ${ElecDevil.TAG}/$name") {
                val (width, height) = atlasRegionSize("${ElecDevil.TAG}/$name")
                withClue("$name is $width x $height, a frame is $frameWidth x $frameHeight") {
                    width % frameWidth shouldBe 0
                    height % frameHeight shouldBe 0
                    (width / frameWidth) * (height / frameHeight) shouldBe FRAMES
                }
            }
        }
    }

    describe("the body animations") {
        it("ends appear within the shortest APPEAR_DUR and plays appear and defeated once, with $FRAMES frames") {
            val fight = ElecDevilRig()
            try {
                fight.spawnBoss()
                val body = fight.rig.spawned.filterIsInstance<ElecDevilBody>().first()
                val animator = body.getComponent(AnimationsComponent::class)!!.getAnimator(ElecDevilBody.TAG) as Animator
                val shortestAppear = listOf(
                    ElecDevilTuning.NORMAL, ElecDevilTuning.NORMAL_PHASE_2, ElecDevilTuning.HARD,
                    ElecDevilTuning.HARD_PHASE_2
                ).minOf { it.appearDur }
                val appear = animator.animations["appear"]
                appear.getDuration() shouldBeLessThanOrEqual shortestAppear + TIME_TOLERANCE
                listOf("appear", "defeated").forEach { name ->
                    val animation = animator.animations[name]
                    withClue(name) {
                        animation.isLooping() shouldBe false
                        animation.setToEnd()
                        animation.getIndex() shouldBe FRAMES - 1
                    }
                }
            } finally {
                fight.dispose()
            }
        }
    }
})
