package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.utils.ObjectSet
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DarknessRoomRulesTest : DescribeSpec({

    // the darkness covers "cave" and "pit"; "hall" and "yard" are lit
    val rooms = ObjectSet<String>().apply {
        add("cave")
        add("pit")
    }

    describe("on player ready") {

        it("darkens when the player spawns inside a covered room") {
            darkModeOnPlayerReady("cave", rooms) shouldBe true
        }

        it("stays clear when the player spawns outside every covered room") {
            darkModeOnPlayerReady("hall", rooms) shouldBe false
        }

        it("stays clear when there is no room at all") {
            darkModeOnPlayerReady(null, rooms) shouldBe false
        }
    }

    describe("at the beginning of a room transition") {

        it("clears when leaving a covered room for an uncovered one") {
            darkModeOnRoomTransBegin("cave", "hall", rooms, darkMode = true) shouldBe false
        }

        it("clears when arriving in an uncovered room from nowhere") {
            darkModeOnRoomTransBegin(null, "hall", rooms, darkMode = true) shouldBe false
        }

        it("leaves the flag alone when entering a covered room") {
            // entering is the end rule's business, not this one's
            darkModeOnRoomTransBegin("hall", "cave", rooms, darkMode = false) shouldBe false
        }

        it("leaves the flag alone when moving between two covered rooms") {
            darkModeOnRoomTransBegin("cave", "pit", rooms, darkMode = true) shouldBe true
        }

        it("leaves the flag alone when moving between two uncovered rooms") {
            darkModeOnRoomTransBegin("hall", "yard", rooms, darkMode = false) shouldBe false
        }

        it("leaves the flag alone when there is no room to move into") {
            darkModeOnRoomTransBegin("cave", null, rooms, darkMode = true) shouldBe true
        }
    }

    describe("at the end of a room transition") {

        it("darkens when arriving in a covered room from an uncovered one") {
            darkModeOnRoomTransEnd("hall", "cave", rooms, darkMode = false) shouldBe true
        }

        it("darkens when arriving in a covered room from nowhere") {
            darkModeOnRoomTransEnd(null, "cave", rooms, darkMode = false) shouldBe true
        }

        it("leaves the flag alone when leaving a covered room") {
            // leaving is the begin rule's business, not this one's
            darkModeOnRoomTransEnd("cave", "hall", rooms, darkMode = true) shouldBe true
        }

        it("leaves the flag alone when moving between two covered rooms") {
            darkModeOnRoomTransEnd("cave", "pit", rooms, darkMode = true) shouldBe true
        }

        it("leaves the flag alone when moving between two uncovered rooms") {
            darkModeOnRoomTransEnd("hall", "yard", rooms, darkMode = false) shouldBe false
        }

        it("leaves the flag alone when there is no room to move into") {
            darkModeOnRoomTransEnd("hall", null, rooms, darkMode = false) shouldBe false
        }
    }

    describe("the asymmetry between the two transition rules") {

        // Both rules run on every transition. Which one actually moves the flag decides whether the darkness has
        // the length of the transition to animate, or whether it changes only once the camera has arrived. Getting
        // this backwards is what makes a transition look torn.

        it("clears at the beginning when walking out of the dark, and not at the end") {
            val atBegin = darkModeOnRoomTransBegin("cave", "hall", rooms, darkMode = true)
            atBegin shouldBe false

            // by the end there is nothing left for the end rule to do
            darkModeOnRoomTransEnd("cave", "hall", rooms, atBegin) shouldBe false
        }

        it("darkens at the end when walking into the dark, and not at the beginning") {
            val atBegin = darkModeOnRoomTransBegin("hall", "cave", rooms, darkMode = false)

            // still clear for the whole transition, so the room being left is never darkened on the way out
            atBegin shouldBe false

            darkModeOnRoomTransEnd("hall", "cave", rooms, atBegin) shouldBe true
        }
    }
})
