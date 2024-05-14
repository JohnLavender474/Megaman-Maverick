package com.megaman.maverick.game

import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.megaman.maverick.game.entities.bosses.BossType
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GamePasswordsTest : DescribeSpec({
    describe("GamePasswords") {
        it("should get blank game password") {
            // if
            val state = GameState()

            // when
            val password = GamePasswords.getGamePassword(state)

            // then
            val expectedPassword = IntArray(36)
            password shouldBe expectedPassword
        }

        it("should load game password - 1") {
            // if
            val state = GameState()
            state.bossesDefeated.addAll(
                BossType.BLUNT_MAN,
                BossType.RODENT_MAN
            )
            state.heartTanksCollected.addAll(
                MegaHeartTank.C
            )
            state.healthTanksCollected.putAll(
                objectMapOf(
                    MegaHealthTank.A to 0,
                    MegaHealthTank.C to 0
                )
            )

            // when
            val password = GamePasswords.getGamePassword(state)
            println(password.joinToString(""))

            // then
            val newState = GameState()
            GamePasswords.loadGamePassword(newState, password)
            newState shouldBe state
        }

        it("should load game password - 2") {
            // if
            val state = GameState()
            state.bossesDefeated.addAll(
                BossType.BLUNT_MAN,
                BossType.RODENT_MAN,
                BossType.MICROWAVE_MAN,
                BossType.ROASTER_MAN,
                BossType.TIMBER_WOMAN
            )
            state.heartTanksCollected.addAll(
                MegaHeartTank.A,
                MegaHeartTank.B,
                MegaHeartTank.C,
                MegaHeartTank.E,
                MegaHeartTank.H
            )
            state.healthTanksCollected.putAll(
                objectMapOf(
                    MegaHealthTank.A to 0,
                    MegaHealthTank.C to 0,
                    MegaHealthTank.D to 0
                )
            )

            // when
            val password = GamePasswords.getGamePassword(state)
            println(password.joinToString(""))

            // then
            val newState = GameState()
            GamePasswords.loadGamePassword(newState, password)
            newState shouldBe state
        }

        it("should load game password - 3") {
            // if
            // bosses defeated = Timber Woman, Rodent Man --> 0, 6
            // heart tanks collected = A, C --> 8, 10
            // health tanks collected = B --> 18
            // indices set to true: 8, 17, 33, 23, 13
            val password = intArrayOf(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                1,
                0,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                1,
                0,
                0,
                0,
                0,
                0,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                1,
                0,
                0,
            )

            // when
            val state = GameState()
            GamePasswords.loadGamePassword(state, password)

            // then
            state.bossesDefeated shouldBe objectSetOf(BossType.TIMBER_WOMAN, BossType.RODENT_MAN)
            state.heartTanksCollected shouldBe objectSetOf(MegaHeartTank.A, MegaHeartTank.C)
            state.healthTanksCollected shouldBe objectMapOf(MegaHealthTank.B to 0)
        }
    }
})