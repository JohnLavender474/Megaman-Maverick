package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.entities.decorations.Splash.SplashType

interface IWater : IBodyEntity {

    fun shouldSplash(fixture: IFixture): Boolean

    fun getSplashType(fixture: IFixture): SplashType

    fun doMakeSplashSound(fixture: IFixture): Boolean
}
