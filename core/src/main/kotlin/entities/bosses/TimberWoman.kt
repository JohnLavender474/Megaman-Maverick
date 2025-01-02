package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.state.StateMachine
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import kotlin.reflect.KClass

class TimberWoman(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "TimberWoman"

        private const val BODY_WIDTH = 1.5f
        private const val BODY_HEIGHT = 1.75f

        private const val SPRITE_SIZE = 3.5f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class TimberWomanState { INIT, STAND, STAND_SWING, RUN, JUMP_UP, JUMP_DOWN, JUMP_SPIN }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(1),
        Fireball::class pairTo dmgNeg(2),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 2 else 1
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 2 else 1
        }
    )
    override lateinit var facing: Facing

    private val timers = OrderedMap<String, Timer>()

    private lateinit var stateMachine: StateMachine<TimberWomanState>
    private val currentState: TimberWomanState
        get() = stateMachine.getCurrent()

    override fun init() {
        if (regions.isEmpty) {
            // TODO
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        putProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, false)
        super.onSpawn(spawnProps)
    }

    override fun isReady(delta: Float) = true // TODO

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
