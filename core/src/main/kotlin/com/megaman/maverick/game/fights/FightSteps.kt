package com.megaman.maverick.game.fights

import com.maverick.game.engine.animations.AnimationsSystem
import com.maverick.game.engine.common.extensions.isAny
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.drawables.sprites.SpritesSystem
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.locks.lock
import com.maverick.game.engine.locks.unlock
import com.maverick.game.engine.players.forEach
import com.maverick.game.engine.state.Step
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.IBossListener
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.locks.MegaLockGroup
import com.megaman.maverick.game.locks.MegaLockReason
import com.megaman.maverick.game.utils.misc.HealthFillType

object AwaitBossReady : Step<Fight> {

    override fun isFinished(context: Fight) = context.boss.ready
}

object HoldWorld : Step<Fight> {

    override fun begin(context: Fight) {
        val engine = context.services.engine

        engine.systems.forEach {
            if (!it.isAny(SpritesSystem::class, AnimationsSystem::class)) it.on = false
        }

        context.lease(FightLease.WORLD_HOLD) { engine.systems.forEach { it.on = true } }
    }

    override fun isFinished(context: Fight) = true
}

object FillBossHealthBar : Step<Fight> {

    override fun begin(context: Fight) {
        val services = context.services
        val bar = context.bar

        bar.set(
            context.boss,
            HealthFillType.BIT_BY_BIT,
            { services.audio.pauseMusic() },
            {
                services.events.submitEvent(Event(EventType.END_BOSS_SPAWN))
                services.audio.playMusic()
            }
        )

        context.lease(FightLease.HEALTH_BAR) { bar.unset() }
    }

    override fun isFinished(context: Fight) = context.bar.finished
}

object ReleaseWorld : Step<Fight> {

    override fun begin(context: Fight) = context.release(FightLease.WORLD_HOLD)

    override fun isFinished(context: Fight) = true
}

object EndBossSpawn : Step<Fight> {

    override fun begin(context: Fight) {
        context.services.events.submitEvent(Event(EventType.END_BOSS_SPAWN))
    }

    override fun isFinished(context: Fight) = true
}

object ControllerOn : Step<Fight> {

    override fun begin(context: Fight) {
        context.services.events.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
    }

    override fun isFinished(context: Fight) = true
}

object BossDefeatLock : Step<Fight> {

    override fun begin(context: Fight) {
        if (!context.leadPlayerAlive) return

        val services = context.services

        services.events.submitEvent(
            Event(
                EventType.TURN_CONTROLLER_OFF, props(
                    "${ConstKeys.CONTROLLER}_${ConstKeys.SYSTEM}_${ConstKeys.OFF}" pairTo false
                )
            )
        )
        services.players.forEach { it.lock(MegaLockGroup.DAMAGE, MegaLockReason.BOSS_DEFEATED) }
        services.audio.unsetMusic()
    }

    override fun isFinished(context: Fight) = true
}

object NotifyListenersOfBossDefeat : Step<Fight> {

    override fun begin(context: Fight) {
        if (!context.leadPlayerAlive) return

        MegaGameEntities.forEach {
            if (it is IBossListener && it.getType() != EntityType.BOSS) it.onBossDefeated(context.boss)
        }
    }

    override fun isFinished(context: Fight) = true
}

object HideBossHealthBar : Step<Fight> {

    override fun begin(context: Fight) {
        if (context.leadPlayerAlive) context.release(FightLease.HEALTH_BAR)
    }

    override fun isFinished(context: Fight) = true
}

object AwaitBossDefeatEnd : Step<Fight> {

    override fun isFinished(context: Fight) = !context.boss.existenceState.liveAfterUpdate
}

object LevelVictory : Step<Fight> {

    override fun begin(context: Fight) {
        if (!context.leadPlayerAlive) return

        context.services.events.submitEvent(
            Event(EventType.VICTORY_EVENT, props(ConstKeys.BOSS pairTo context.boss))
        )
    }

    override fun isFinished(context: Fight) = context.leadPlayerAlive
}

object ExitMidBossFight : Step<Fight> {

    override fun begin(context: Fight) {
        if (!context.leadPlayerAlive) return

        context.services.events.submitEvent(
            Event(EventType.INTERMEDIATE_BOSS_DEAD, props(ConstKeys.BOSS pairTo context.boss))
        )
    }

    override fun isFinished(context: Fight) = context.leadPlayerAlive
}

object ResumeAfterMidBoss : Step<Fight> {

    override fun begin(context: Fight) {
        val services = context.services

        services.events.submitEvent(Event(EventType.TURN_CONTROLLER_ON))
        services.players.forEach { it.unlock(MegaLockGroup.DAMAGE, MegaLockReason.BOSS_DEFEATED) }
        services.audio.playMusic(context.levelMusic(), true)
    }

    override fun isFinished(context: Fight) = true
}

object AwaitBossGone : Step<Fight> {

    override fun isFinished(context: Fight) = !context.boss.existenceState.liveNow
}
