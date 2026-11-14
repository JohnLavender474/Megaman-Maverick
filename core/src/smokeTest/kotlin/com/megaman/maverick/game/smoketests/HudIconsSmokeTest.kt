package com.megaman.maverick.game.smoketests

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.protoman.ProtomanWeapon
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainAll
import java.io.File

class HudIconsSmokeTest : DescribeSpec({

    describe("the HUD atlas") {
        it("has the slot icon of each Proto Man weapon") {
            val file = FileHandle(File(TextureAsset.HUD.source))
            val data = TextureAtlas.TextureAtlasData(file, file.parent(), false)
            val regions = data.regions.map { it.name }

            regions shouldContainAll ProtomanWeapon.entries.map { it.hudRegion }
        }
    }
})
