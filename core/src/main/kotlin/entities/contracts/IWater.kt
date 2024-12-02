package com.megaman.maverick.game.entities.contracts

import com.mega.game.engine.entities.contracts.IBodyEntity

interface IWater: IBodyEntity {

    fun doMakeSplashSound(): Boolean
}
