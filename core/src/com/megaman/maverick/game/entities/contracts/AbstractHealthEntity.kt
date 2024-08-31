package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.GameLogger
import com.engine.common.objects.Properties
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.points.PointsComponent
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.damage.DamageNegotiation
import kotlin.reflect.KClass

abstract class AbstractHealthEntity(
    game: MegamanMaverickGame,
    dmgDuration: Float = DEFAULT_DMG_DURATION,
    dmgBlinkDur: Float = DEFAULT_DMG_BLINK_DUR
) : MegaGameEntity(game), IHealthEntity, IDamageable {

    companion object {
        const val DEFAULT_DMG_DURATION = 0.1f
        const val DEFAULT_DMG_BLINK_DUR = 0.05f
    }

    override val invincible: Boolean
        get() = !damageTimer.isFinished()

    protected abstract val damageNegotiations: ObjectMap<KClass<out IDamager>, DamageNegotiation>
    protected val damageTimer = Timer(dmgDuration)
    protected val damageBlinkTimer = Timer(dmgBlinkDur)
    protected var damageBlink = false

    override fun init() {
        super.init()
        addComponent(definePointsComponent())
        val updatablesComponent = UpdatablesComponent()
        addComponent(updatablesComponent)
        defineUpdatablesComponent(updatablesComponent)
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        setHealth(ConstVals.MAX_HEALTH)
        damageTimer.setToEnd()
        damageBlinkTimer.setToEnd()
    }

    override fun canBeDamagedBy(damager: IDamager) = !invincible && damageNegotiations.containsKey(damager::class)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damagerKey = damager::class
        if (!damageNegotiations.containsKey(damagerKey)) return false

        damageTimer.reset()

        val damage = damageNegotiations[damagerKey].get(damager)
        if (damage <= 0) return false

        translateHealth(-damage)

        return true
    }


    protected open fun definePointsComponent(): PointsComponent {
        val pointsComponent = PointsComponent()
        pointsComponent.putPoints(
            ConstKeys.HEALTH, max = ConstVals.MAX_HEALTH, current = ConstVals.MAX_HEALTH, min = ConstVals.MIN_HEALTH
        )
        pointsComponent.putListener(ConstKeys.HEALTH) {
            if (it.current <= ConstVals.MIN_HEALTH) {
                GameLogger.debug(AbstractEnemy.TAG, "Kill enemy due to depleted health")
                kill()
            }
        }
        return pointsComponent
    }

    protected open fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        updatablesComponent.add {
            damageTimer.update(it)
            if (!damageTimer.isFinished()) {
                damageBlinkTimer.update(it)
                if (damageBlinkTimer.isFinished()) {
                    damageBlinkTimer.reset()
                    damageBlink = !damageBlink
                }
            } else damageBlink = false
        }
    }
}