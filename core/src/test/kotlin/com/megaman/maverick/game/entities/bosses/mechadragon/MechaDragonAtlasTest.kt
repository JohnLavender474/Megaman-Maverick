package com.megaman.maverick.game.entities.bosses.mechadragon

import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.bosses.MechaDragon
import com.megaman.maverick.game.entities.projectiles.MechaDragonFlame
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.File
import javax.imageio.ImageIO

private const val FRAMES_DIR = "../assets/sprites/frames/MechaDragon"
private const val ASSETS_DIR = "../assets"

private class Strip(val rows: Int, val cols: Int, val cellWidth: Int, val cellHeight: Int)

private val AIR = MechaDragonArt.AIR_CELL_HEIGHT
private val GROUND = MechaDragonArt.GROUND_CELL_HEIGHT
private val WIDTH = MechaDragonArt.CELL_WIDTH

private val STRIPS = linkedMapOf(
    "init" to Strip(1, 1, WIDTH, AIR),
    "fly" to Strip(3, 2, WIDTH, AIR),
    "fire1" to Strip(2, 5, WIDTH, AIR),
    "fire2" to Strip(3, 2, WIDTH, AIR),
    "turning" to Strip(5, 1, WIDTH, AIR),
    "charging" to Strip(3, 2, WIDTH, AIR),
    "defeated" to Strip(2, 2, WIDTH, AIR),
    "frozen" to Strip(1, 1, WIDTH, AIR),
    "land" to Strip(1, 4, WIDTH, GROUND),
    "stand" to Strip(1, 2, WIDTH, GROUND),
    "breath_windup" to Strip(1, 3, WIDTH, GROUND),
    "breath_sweep" to Strip(1, MechaDragonArt.SWEEP_ANGLES.size, WIDTH, GROUND),
    "breath_end" to Strip(1, 2, WIDTH, GROUND),
    "chomp" to Strip(1, MechaDragonArt.CHOMP_HEAD_BOXES.size, WIDTH, GROUND)
)

private fun atlasRegions(): Map<String, Pair<Int, Int>> {
    val lines = File("$ASSETS_DIR/${TextureAsset.MECHA_DRAGON.source}").readLines()
    val regions = LinkedHashMap<String, Pair<Int, Int>>()
    var name: String? = null
    lines.drop(1).forEach { line ->
        when {
            !line.startsWith(" ") && !line.contains(":") -> name = line
            line.trim().startsWith("size:") && name != null -> {
                val (w, h) = line.substringAfter(":").split(",").map { it.trim().toInt() }
                regions[name!!] = w to h
                name = null
            }
        }
    }
    return regions
}

class MechaDragonAtlasTest : DescribeSpec({

    describe("the MechaDragon frames and atlas") {
        it("each frame strip has the layout that the code cuts it with") {
            STRIPS.forEach { (key, strip) ->
                val image = ImageIO.read(File("$FRAMES_DIR/${MechaDragon.TAG}/$key.png"))
                withClue(key) {
                    image.width shouldBe strip.cols * strip.cellWidth
                    image.height shouldBe strip.rows * strip.cellHeight
                }
            }
        }

        it("the flame puff strip has 4 cells of 16x16") {
            val image = ImageIO.read(File("$FRAMES_DIR/${MechaDragonFlame.TAG}.png"))
            image.width shouldBe 4 * 16
            image.height shouldBe 16
        }

        it("the atlas has a region of the same size for every strip that the dragon and the flame request") {
            val regions = atlasRegions()
            STRIPS.forEach { (key, strip) ->
                withClue(key) { regions["${MechaDragon.TAG}/$key"] shouldBe (strip.cols * strip.cellWidth to strip.rows * strip.cellHeight) }
            }
            regions[MechaDragonFlame.TAG] shouldBe (64 to 16)
        }

        it("the atlas holds nothing else, so a stale region cannot hide a missing frame") {
            atlasRegions().keys shouldBe (STRIPS.keys.map { "${MechaDragon.TAG}/$it" } + MechaDragonFlame.TAG).toSet()
        }
    }
})
