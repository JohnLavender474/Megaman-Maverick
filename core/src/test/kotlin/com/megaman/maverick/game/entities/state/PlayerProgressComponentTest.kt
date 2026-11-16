package com.megaman.maverick.game.entities.state

import com.megaman.maverick.game.entities.megaman.constants.MegaEnhancement
import com.megaman.maverick.game.entities.megaman.constants.MegaHealthTank
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.levels.LevelDefinition
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PlayerProgressComponentTest : DescribeSpec({

    var heartTankChanges = 0
    var currencyAdds = 0

    val component = PlayerProgressComponent(
        onHeartTanksChanged = { heartTankChanges++ },
        onCurrencyAdded = { currencyAdds++ }
    )

    beforeEach {
        heartTankChanges = 0
        currencyAdds = 0
    }

    describe("PlayerProgressComponent") {
        it("calls the heart tank hook once for an added and for a removed tank") {
            component.onAddHeartTank(MegaHeartTank.A)
            heartTankChanges shouldBe 1

            component.onRemoveHeartTank(MegaHeartTank.A)
            heartTankChanges shouldBe 2
        }

        it("calls the currency hook once for added currency") {
            component.onAddCurrency(5)

            currencyAdds shouldBe 1
        }

        it("calls no hook for the other members") {
            component.onAddLevelCompleted(LevelDefinition.DESERT_MAN)
            component.onRemoveLevelCompleted(LevelDefinition.DESERT_MAN)
            component.onPutHealthTank(MegaHealthTank.A)
            component.onRemoveHealthTank(MegaHealthTank.A)
            component.onAddHealthToHealthTank(MegaHealthTank.A, 1)
            component.onRemoveHealthFromHealthTank(MegaHealthTank.A, 1)
            component.onAddEnhancement(MegaEnhancement.entries.first())
            component.onRemoveEnhancement(MegaEnhancement.entries.first())
            component.onRemoveCurrency(5)

            heartTankChanges shouldBe 0
            currencyAdds shouldBe 0
        }
    }
})
