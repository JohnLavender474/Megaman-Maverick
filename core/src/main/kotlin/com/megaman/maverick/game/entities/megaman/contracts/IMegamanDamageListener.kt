package com.megaman.maverick.game.entities.megaman.contracts

import com.mega.game.engine.damage.IDamager
import com.megaman.maverick.game.entities.megaman.Megaman

interface IMegamanDamageListener {

    fun onMegamanDamaged(damager: IDamager, megaman: Megaman)
}
