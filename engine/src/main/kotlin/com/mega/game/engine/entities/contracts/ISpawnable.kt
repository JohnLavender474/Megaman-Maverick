package com.mega.game.engine.entities.contracts

import com.mega.game.engine.common.objects.Properties

interface ISpawnable {

    fun spawn(spawnProps: Properties): Boolean
}
