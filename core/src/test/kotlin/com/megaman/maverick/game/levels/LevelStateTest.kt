package com.megaman.maverick.game.levels

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.MegamanMaverickGame
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class LevelStateTest : DescribeSpec({

    describe("LevelState") {
        it("starts with the values that the game gave for a missing key") {
            val state = LevelState()
            state.currentLevel.shouldBeNull()
            state.roomsSupplier.shouldBeNull()
            state.currentRoomSupplier.shouldBeNull()
            state.worldContainer.shouldBeNull()
            state.tiledMapLoadResult.shouldBeNull()
            state.roomTransition shouldBe false
            state.megamanBeam shouldBe false
            state.megamanBeamCenter.shouldBeNull()
            state.coins shouldBe 0
            state.levelPoints shouldBe 0
            state.timeSinceLastFloatingPoints shouldBe 0f
            state.lastFloatingPointsType.shouldBeNull()
        }

        it("keeps the beam center object that was set, not a copy") {
            val state = LevelState()
            val center = Vector2(1f, 2f)
            state.megamanBeamCenter = center
            center.set(3f, 4f)
            state.megamanBeamCenter shouldBeSameInstanceAs center
        }
    }

    describe("the game functions before the first write") {
        val game = MegamanMaverickGame()

        it("getCurrentLevel and getTiledMapLoadResult throw") {
            shouldThrow<NullPointerException> { game.getCurrentLevel() }
            shouldThrow<NullPointerException> { game.getTiledMapLoadResult() }
        }

        it("the other reads give the empty values") {
            game.isInExtraLevel() shouldBe false
            val out = Array<RectangleMapObject>()
            game.getRooms(out) shouldBeSameInstanceAs out
            out.size shouldBe 0
            game.getCurrentRoom().shouldBeNull()
            game.getWorldContainer().shouldBeNull()
            game.isCameraRotating() shouldBe false
            game.isFocusSnappedAway() shouldBe false
            game.weaponsAttained.shouldBeNull()
        }
    }

    describe("the game functions after a write") {
        it("call the room suppliers at each read and do not copy the rooms") {
            val game = MegamanMaverickGame()
            val rooms = Array<RectangleMapObject>()
            var current: RectangleMapObject? = null
            game.setRoomsSupplier { rooms }
            game.setCurrentRoomSupplier { current }

            game.getRooms(Array()).size shouldBe 0
            game.getCurrentRoom().shouldBeNull()

            val room = RectangleMapObject()
            rooms.add(room)
            current = room

            game.getRooms(Array()).first() shouldBeSameInstanceAs room
            game.getCurrentRoom() shouldBeSameInstanceAs room
        }

        it("keep the level values when a new level is set") {
            val game = MegamanMaverickGame()
            val rooms = Array<RectangleMapObject>().apply { add(RectangleMapObject()) }
            game.setCurrentLevel(LevelDefinition.TEST_1)
            game.setRoomsSupplier { rooms }

            game.setCurrentLevel(LevelDefinition.GAUNTLET_1)

            game.getCurrentLevel() shouldBe LevelDefinition.GAUNTLET_1
            game.isInExtraLevel() shouldBe true
            game.getRooms(Array()).size shouldBe 1
        }
    }
})
