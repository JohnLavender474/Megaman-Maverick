package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.math.Vector2
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.projectiles.JunkShot
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.sqrt

class X6IntroEnemiesTest : DescribeSpec({

    describe("HoverGunner.aimIndex") {

        it("gives 0 for right, 4 for up, 8 for left and 12 for down") {
            HoverGunner.aimIndex(10f, 0f) shouldBe 0
            HoverGunner.aimIndex(0f, 10f) shouldBe 4
            HoverGunner.aimIndex(-10f, 0f) shouldBe 8
            HoverGunner.aimIndex(0f, -10f) shouldBe 12
        }

        it("rounds to the nearest of 16 directions") {
            HoverGunner.aimIndex(10f, 10f) shouldBe 2
            HoverGunner.aimIndex(-10f, -4f) shouldBe 9
        }
    }

    describe("HoverGunner.fanOrder") {

        it("for an aim straight left takes the directions 22.5 degrees above and below, the upper one first") {
            HoverGunner.fanOrder(8).toList() shouldBe listOf(8, 7, 8, 9, 8, 7, 8, 9, 8)
        }

        it("for an aim up-left takes up and left, up first") {
            HoverGunner.fanOrder(6).toList() shouldBe listOf(6, 4, 6, 8, 6, 4, 6, 8, 6)
        }

        it("for an aim down-right takes right and down, right first") {
            HoverGunner.fanOrder(14).toList() shouldBe listOf(14, 0, 14, 12, 14, 0, 14, 12, 14)
        }
    }

    describe("HoverGunner.pose") {

        it("maps the axes and the diagonals to the five poses") {
            HoverGunner.pose(8) shouldBe "aim_forward"
            HoverGunner.pose(0) shouldBe "aim_forward"
            HoverGunner.pose(4) shouldBe "aim_up"
            HoverGunner.pose(12) shouldBe "aim_down"
            HoverGunner.pose(6) shouldBe "aim_up_diagonal"
            HoverGunner.pose(7) shouldBe "aim_up_diagonal"
            HoverGunner.pose(10) shouldBe "aim_down_diagonal"
        }

        it("puts the muzzle at the barrel tip of the pose, on the side the gunner faces") {
            val out = Vector2()
            val offset = HoverGunner.SPRITE_OFFSET_X * ConstVals.PPM
            HoverGunner.muzzleOffset("aim_forward", false, out) shouldBe Vector2(-(offset + 14f), 2f)
            HoverGunner.muzzleOffset("aim_forward", true, out) shouldBe Vector2(offset + 14f, 2f)
            HoverGunner.muzzleOffset("aim_up", true, out) shouldBe Vector2(offset, 14f)
            HoverGunner.muzzleOffset("aim_down_diagonal", false, out) shouldBe Vector2(-(offset + 12f), -10f)
        }

        it("refuses a pose with no barrel") {
            shouldThrow<IllegalArgumentException> { HoverGunner.muzzleOffset("fly", false, Vector2()) }
        }
    }

    describe("Junkroid.inRange") {

        it("is true closer than 7 tiles on either side and false at 7 tiles or more") {
            Junkroid.inRange(6.9f * ConstVals.PPM) shouldBe true
            Junkroid.inRange(-6.9f * ConstVals.PPM) shouldBe true
            Junkroid.inRange(7f * ConstVals.PPM) shouldBe false
            Junkroid.inRange(-7f * ConstVals.PPM) shouldBe false
        }
    }

    describe("Junkroid hurl") {

        it("puts the throw point at the eye slit of the shoot frames, on both sides") {
            Junkroid.mouthOffset(-1f, Vector2()) shouldBe Vector2(3f, 19f)
            Junkroid.mouthOffset(1f, Vector2()) shouldBe Vector2(-3f, 19f)
        }

        it("throws up and forward on the side that the Junkroid faces") {
            Junkroid.hurlImpulse(1f, Vector2()) shouldBe
                Vector2(Junkroid.HURL_X * ConstVals.PPM, Junkroid.HURL_Y * ConstVals.PPM)
            Junkroid.hurlImpulse(-1f, Vector2()) shouldBe
                Vector2(-Junkroid.HURL_X * ConstVals.PPM, Junkroid.HURL_Y * ConstVals.PPM)
        }

        it("lands on the floor within 1 tile of the edge of the attack range") {
            val impulse = Junkroid.hurlImpulse(1f, Vector2())
            val start = Junkroid.mouthOffset(1f, Vector2())
            val drop = Junkroid.ORIGIN_ABOVE_FEET * ConstVals.PPM + start.y
            val fall = -Junkroid.HURL_GRAVITY * ConstVals.PPM
            val time = (impulse.y + sqrt(impulse.y * impulse.y + 2f * fall * drop)) / fall
            val landX = (start.x + impulse.x * time) / ConstVals.PPM
            (landX > Junkroid.RANGE - 1f) shouldBe true
            (landX < Junkroid.RANGE + 1f) shouldBe true
        }
    }

    describe("MetaDridler drill bit") {

        listOf("wait", "drill", "rise").forEach { key ->
            it("turns in every frame of $key: on screen the thread moves down 1 row per frame") {
                val def = MetaDridler.FRAMES[key]!!
                val strip = ImageIO.read(File("../assets/sprites/frames/Enemies2/MetaDridler/$key.png"))
                strip.height shouldBe def.count * def.height

                fun column(frame: Int) = (42..58).map { strip.getRGB(15, frame * def.height + it) }

                (0 until def.count).forEach { frame ->
                    val now = column(frame)
                    val next = column((frame + 1) % def.count)
                    next.drop(1) shouldBe now.dropLast(1)
                }
            }
        }
    }

    describe("JunkShot gravity") {

        it("pulls only the Junkroid junk down") {
            JunkShot.Type.JUNKROID.gravity shouldBe Junkroid.HURL_GRAVITY
            JunkShot.Type.JUNKROID_HARD.gravity shouldBe Junkroid.HURL_GRAVITY
            JunkShot.Type.HOVER_GUNNER.gravity shouldBe 0f
            JunkShot.Type.CORE.gravity shouldBe 0f
        }
    }

    describe("MetaWheelF picture index") {

        it("keeps the same picture when the roll direction or the flash strip changes") {
            (0 until MetaWheelF.FRAME_COUNT).forEach { picture ->
                listOf("roll_left", "roll_right", "turn_flash").forEach { key ->
                    MetaWheelF.picture(key, MetaWheelF.indexFor(key, picture)) shouldBe picture
                }
            }
            MetaWheelF.indexFor("roll_right", 0) shouldBe MetaWheelF.FRAME_COUNT - 1
        }
    }
})
