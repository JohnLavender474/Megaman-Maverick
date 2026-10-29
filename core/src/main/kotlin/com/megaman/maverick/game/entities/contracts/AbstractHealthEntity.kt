package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.points.Points
import com.maverick.game.engine.points.PointsComponent
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.IDamageNegotiator
import kotlin.reflect.KClass

abstract class AbstractHealthEntity(
    game: MegaServices,
    protected var dmgDuration: Float = DEFAULT_DMG_DURATION,
    dmgBlinkDur: Float = DEFAULT_DMG_BLINK_DUR
) : MegaGameEntity(game), IHealthEntity, IDamageableEntity {

    companion object {
        const val DEFAULT_DMG_DURATION = 0.1f
        const val DEFAULT_DMG_BLINK_DUR = 0.05f
    }

    override val invincible: Boolean
        get() = !damageTimer.isFinished() || invinciblePredicates.any { it.invoke() }

    val damaged: Boolean
        get() = !damageTimer.isFinished()

    val onDamagedCallbacks = Array<(IDamagerEntity, Int) -> Unit>()
    val onHealthDepletedCallbacks = Array<() -> Unit>()
    val invinciblePredicates: Array<() -> Boolean> = Array()

    protected abstract val damageNegotiator: IDamageNegotiator?
    protected open val damageOverrides = ObjectMap<KClass<out IDamagerEntity>, DamageNegotiation?>()

    protected open val damageTimer = Timer(dmgDuration)
    protected open val damageBlinkTimer = Timer(dmgBlinkDur)

    protected var lastDamager: IDamagerEntity? = null

    open var damageBlink = false
        protected set

    private var wasHealthDepleted = false

    override fun init(vararg params: Any) {
        super.init()
        addComponent(definePointsComponent())
        val updatablesComponent = UpdatablesComponent()
        addComponent(updatablesComponent)
        defineUpdatablesComponent(updatablesComponent)
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        setHealthToMax()

        damageTimer.setToEnd()
        damageBlinkTimer.setToEnd()

        wasHealthDepleted = false
    }

    override fun onDestroy() {
        super.onDestroy()
        lastDamager = null
    }

    override fun canBeDamagedBy(damager: IDamagerEntity) = existenceState.liveAfterUpdate && !invincible &&
        (damageOverrides.containsKey(damager::class) ||
            (damageNegotiator != null && damageNegotiator!!.get(damager) != 0))

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        val negotiation = when {
            damageOverrides.containsKey(damager::class) -> damageOverrides[damager::class]?.get(damager) ?: 0
            else -> damageNegotiator?.get(damager) ?: 0
        }

        if (negotiation <= 0) return false

        val editedDamage = editDamageFrom(damager, negotiation)
        if (editedDamage <= 0) return false

        translateHealth(-editedDamage)

        val dmgDur = getDamageDuration(damager)
        damageTimer.resetDuration(dmgDur)

        lastDamager = damager

        onDamaged(damager, editedDamage)

        return true
    }

    protected open fun onDamaged(damager: IDamagerEntity, damage: Int) {
        onDamagedCallbacks.forEach { it.invoke(damager, damage) }
    }

    protected open fun getDamageDuration(damager: IDamagerEntity) = dmgDuration

    protected open fun editDamageFrom(damager: IDamagerEntity, baseDamage: Int) = baseDamage

    protected open fun onDamageFinished() {}

    protected open fun onHealthDepleted() {
        onHealthDepletedCallbacks.forEach { it.invoke() }
        destroy()
    }

    protected open fun definePointsComponent(): PointsComponent {
        val pointsComponent = PointsComponent()

        val points = Points(max = ConstVals.MAX_HEALTH, current = ConstVals.MAX_HEALTH, min = ConstVals.MIN_HEALTH)
        pointsComponent.putPoints(ConstKeys.HEALTH, points)

        pointsComponent.putListener(ConstKeys.HEALTH) {
            if (it.current <= ConstVals.MIN_HEALTH && !wasHealthDepleted) {
                wasHealthDepleted = true
                onHealthDepleted()
            }
        }

        return pointsComponent
    }

    protected open fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        updatablesComponent.add { delta ->
            damageTimer.update(delta)
            when {
                !damageTimer.isFinished() -> {
                    damageBlinkTimer.update(delta)
                    if (damageBlinkTimer.isFinished()) {
                        damageBlinkTimer.reset()
                        damageBlink = !damageBlink
                    }
                }
                else -> {
                    if (damageTimer.isJustFinished()) onDamageFinished()
                    damageBlink = false
                }
            }
        }
    }
}
