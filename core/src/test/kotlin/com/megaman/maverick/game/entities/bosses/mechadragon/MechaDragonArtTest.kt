package com.megaman.maverick.game.entities.bosses.mechadragon

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.megaman.maverick.game.ConstVals
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.abs

class MechaDragonArtTest : DescribeSpec({

    val px = MechaDragonArt.GAME_PX_PER_ART_PX

    describe("the sweep angle to frame map") {
        it("shows the drawn frame of each drawn angle") {
            MechaDragonArt.SWEEP_ANGLES.forEachIndexed { i, angle -> MechaDragonArt.sweepCell(angle) shouldBe i }
        }

        it("shows the nearest drawn frame between the drawn angles and past the ends") {
            MechaDragonArt.sweepCell(40f) shouldBe 0
            MechaDragonArt.sweepCell(20f) shouldBe 1
            MechaDragonArt.sweepCell(6f) shouldBe 2
            MechaDragonArt.sweepCell(-8f) shouldBe 3
            MechaDragonArt.sweepCell(-25f) shouldBe 4
            MechaDragonArt.sweepCell(-40f) shouldBe 5
            MechaDragonArt.sweepCell(-60f) shouldBe 5
        }

        it("has a mouth point and a head box for each drawn angle") {
            MechaDragonArt.SWEEP_MOUTHS.size shouldBe MechaDragonArt.SWEEP_ANGLES.size
            MechaDragonArt.SWEEP_HEAD_BOXES.size shouldBe MechaDragonArt.SWEEP_ANGLES.size
        }
    }

    describe("the mouth anchors") {
        it("puts the mouth at the center of its art pixel, y down in the art, from the ground body center") {
            val mouth = MechaDragonArt.SWEEP_MOUTHS[2]
            val offset = MechaDragonArt.pixelOffset(mouth, MechaDragonArt.GROUND_CENTER_ROW, Facing.RIGHT, Vector2())
            offset.x shouldBe ((mouth.x + 0.5f - 64f) * px plusOrMinus 0.001f)
            offset.y shouldBe ((MechaDragonArt.GROUND_CENTER_ROW - mouth.y - 0.5f) * px plusOrMinus 0.001f)
        }

        it("mirrors x to 127 - x when the dragon faces left") {
            MechaDragonArt.SWEEP_MOUTHS.forEach { mouth ->
                val left = MechaDragonArt.pixelOffset(mouth, MechaDragonArt.GROUND_CENTER_ROW, Facing.LEFT, Vector2())
                val mirrored = MechaDragonArt.pixelOffset(
                    ArtPoint(127 - mouth.x, mouth.y), MechaDragonArt.GROUND_CENTER_ROW, Facing.RIGHT, Vector2()
                )
                withClue("${mouth.x},${mouth.y}") {
                    left.x shouldBe (mirrored.x plusOrMinus 0.001f)
                    left.y shouldBe (mirrored.y plusOrMinus 0.001f)
                }
            }
        }

        it("puts the 0 degree mouth over the near edge of the center block: about 2.5 tiles ahead of the feet, 5 up") {
            val target = Vector2(3f * ConstVals.PPM, 0f)
            val center = MechaDragonArt.groundCenter(target, Facing.RIGHT, Vector2())
            val mouth = MechaDragonArt.pixelOffset(
                MechaDragonArt.SWEEP_MOUTHS[2], MechaDragonArt.GROUND_CENTER_ROW, Facing.RIGHT, Vector2()
            ).add(center)
            ((mouth.x - target.x) / ConstVals.PPM) shouldBe (2.5f plusOrMinus 0.1f)
            ((mouth.y - target.y) / ConstVals.PPM) shouldBe (5.3f plusOrMinus 0.1f)
        }
    }

    describe("the ground placement") {
        it("puts the feet anchor on the land point and the cell bottom on the ledge, for both facings") {
            val target = Vector2(100f, 200f)
            Facing.entries.forEach { facing ->
                val center = MechaDragonArt.groundCenter(target, facing, Vector2())
                val anchor = MechaDragonArt.artToOffset(
                    MechaDragonArt.FEET_ANCHOR_X.toFloat(), MechaDragonArt.GROUND_CELL_HEIGHT.toFloat(),
                    MechaDragonArt.GROUND_CENTER_ROW, facing, Vector2()
                ).add(center)
                withClue(facing) {
                    anchor.x shouldBe (target.x plusOrMinus 0.001f)
                    anchor.y shouldBe (target.y plusOrMinus 0.001f)
                }
                val bottom = center.y - MechaDragonArt.spriteBottomBelowCenter(
                    MechaDragonArt.GROUND_CENTER_ROW, MechaDragonArt.GROUND_CELL_HEIGHT
                )
                bottom shouldBe (target.y plusOrMinus 0.001f)
            }
        }

        it("keeps every draw offset on the NES pixel grid (whole multiples of 2 game px)") {
            val offsets = gdxArrayOf(
                MechaDragonArt.spriteBottomBelowCenter(MechaDragonArt.AIR_CENTER_ROW, MechaDragonArt.AIR_CELL_HEIGHT),
                MechaDragonArt.spriteBottomBelowCenter(
                    MechaDragonArt.GROUND_CENTER_ROW, MechaDragonArt.GROUND_CELL_HEIGHT
                ),
                MechaDragonArt.standingSpriteBottomBelowCenter(MechaDragonArt.FROZEN_FOOT_ROW),
                MechaDragonArt.standingSpriteBottomBelowCenter(MechaDragonArt.DEFEATED_FOOT_ROW),
                MechaDragonArt.groundCenter(Vector2(), Facing.RIGHT, Vector2()).x,
                MechaDragonArt.CELL_WIDTH * px / 2f
            )
            offsets.forEach { offset -> withClue(offset) { (abs(offset) % 2f) shouldBe 0f } }
        }

        it("keeps the airborne sprite top where the old 8x8 sprite had it: 4 tiles over the body center") {
            val top = -MechaDragonArt.spriteBottomBelowCenter(
                MechaDragonArt.AIR_CENTER_ROW, MechaDragonArt.AIR_CELL_HEIGHT
            ) + MechaDragonArt.AIR_CELL_HEIGHT * px
            top shouldBe (4f * ConstVals.PPM plusOrMinus 0.001f)
        }

        it("stands a frozen or defeated airborne pose on its lowest foot row at the ledge") {
            val center = 0f
            val ledge = center - MechaDragonArt.spriteBottomBelowCenter(
                MechaDragonArt.GROUND_CENTER_ROW, MechaDragonArt.GROUND_CELL_HEIGHT
            )
            val bottom = center - MechaDragonArt.standingSpriteBottomBelowCenter(MechaDragonArt.FROZEN_FOOT_ROW)
            val footBottom = bottom + (MechaDragonArt.AIR_CELL_HEIGHT - 1 - MechaDragonArt.FROZEN_FOOT_ROW) * px
            footBottom shouldBe (ledge plusOrMinus 0.001f)
        }
    }

    describe("the cell at a time") {
        it("follows the frame rule of Animation: a cell holds until its duration has fully passed") {
            val durations = gdxArrayOf(0.45f, 0.06f, 0.3f, 0.15f)
            MechaDragonArt.cellAt(durations, 0f) shouldBe 0
            MechaDragonArt.cellAt(durations, 0.45f) shouldBe 0
            MechaDragonArt.cellAt(durations, 0.46f) shouldBe 1
            MechaDragonArt.cellAt(durations, 0.6f) shouldBe 2
            MechaDragonArt.cellAt(durations, 0.9f) shouldBe 3
            MechaDragonArt.cellAt(durations, 5f) shouldBe 3
        }

        it("puts the chomp head on the floor only in cells 2 and 3") {
            val floorRow = MechaDragonArt.GROUND_CELL_HEIGHT / 2
            MechaDragonArt.CHOMP_HEAD_BOXES.forEachIndexed { i, box ->
                withClue(i) { (box.y + box.height > floorRow + 40) shouldBe (i == 1 || i == 2) }
            }
        }
    }
})
