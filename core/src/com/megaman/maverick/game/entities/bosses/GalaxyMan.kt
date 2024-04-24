package com.megaman.maverick.game.entities.bosses

import com.engine.common.enums.Facing
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.objects.Properties
import com.engine.damage.IDamager
import com.engine.drawables.sprites.SpritesComponent
import com.engine.world.BodyComponent
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import kotlin.reflect.KClass

class GalaxyMan(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    override lateinit var facing: Facing

    override fun init() {
        super.init()
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
    }

    override fun defineBodyComponent(): BodyComponent {
        TODO("Not yet implemented")
    }

    override fun defineSpritesComponent(): SpritesComponent {
        TODO("Not yet implemented")
    }
}