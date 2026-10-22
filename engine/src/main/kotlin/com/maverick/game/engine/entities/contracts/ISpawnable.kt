package com.maverick.game.engine.entities.contracts

import com.maverick.game.engine.common.objects.Properties

interface ISpawnable {

    fun spawn(spawnProps: Properties): Boolean
}
