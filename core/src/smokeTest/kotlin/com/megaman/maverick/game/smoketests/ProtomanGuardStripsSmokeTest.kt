package com.megaman.maverick.game.smoketests

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.protoman.ProtomanValues
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.File

class ProtomanGuardStripsSmokeTest : DescribeSpec({

    describe("the Proto Man atlas") {
        val file = FileHandle(File(TextureAsset.PROTOMAN.source))
        val regions = TextureAtlas.TextureAtlasData(file, file.parent(), false).regions.associateBy { it.name }

        mapOf(
            "guard_crouch_start" to ProtomanValues.GUARD_CROUCH_TURN_FRAMES,
            "guard_crouch" to 2,
            "guard_crouch_end" to ProtomanValues.GUARD_CROUCH_TURN_FRAMES,
            "guard_jump_start" to ProtomanValues.GUARD_JUMP_TURN_FRAMES,
            "guard_jump" to 1,
            "guard_jump_end" to ProtomanValues.GUARD_JUMP_TURN_FRAMES
        ).forEach { (name, frames) ->
            it("has the strip $name with the frame count of the code") {
                regions[name]!!.height shouldBe frames * ProtomanValues.ART_FRAME_PX
            }
        }

        it("has the crouch attack strips: crouch_shoot 1 frame, crouch_slash 4, crouch_slash_arc 4 of 106x40") {
            regions["crouch_shoot"]!!.height shouldBe ProtomanValues.ART_FRAME_PX
            regions["crouch_slash"]!!.height shouldBe 4 * ProtomanValues.ART_FRAME_PX
            regions["crouch_slash_arc"]!!.width shouldBe 106
            regions["crouch_slash_arc"]!!.height shouldBe 4 * 40
        }

        it("has no ground guard stand strips") {
            regions.containsKey("guard_stand_start") shouldBe false
            regions.containsKey("guard_stand") shouldBe false
        }
    }
})
