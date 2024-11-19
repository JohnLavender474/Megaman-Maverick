package com.megaman.maverick.game.entities.bosses


import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import kotlin.reflect.KClass

class TimberWoman(game: MegamanMaverickGame): AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "TimberWoman"
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    override lateinit var facing: Facing

    override fun init() {
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        putProperty(ConstKeys.ENTTIY_KILLED_BY_DEATH_FIXTURE, false)
        super.onSpawn(spawnProps)
    }

    override fun defineBodyComponent(): BodyComponent {
        TODO("Not yet implemented")
    }

    override fun defineSpritesComponent(): SpritesComponent {
        TODO("Not yet implemented")
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        TODO()
    }
}
