package com.megaman.maverick.game.fights

import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.state.Step
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.entities.bosses.BigAssMaverickRobot
import com.megaman.maverick.game.entities.bosses.MoonMan
import com.megaman.maverick.game.entities.bosses.ReactorMan
import com.megaman.maverick.game.entities.bosses.TimberWoman

object FightPlans {

    fun find(bossName: String, props: Properties): FightPlan? {
        val music = when (bossName) {
            BigAssMaverickRobot.TAG -> MusicAsset.VINNYZ_INTRO_BOSS_MUSIC_INTRO
            TimberWoman.TAG, MoonMan.TAG, ReactorMan.TAG -> null
            else -> return null
        }

        val end = props.getOrDefault(ConstKeys.END, true, Boolean::class)
        return if (end) standardBossPlan(music) else midBossPlan(music)
    }

    fun resolve(plan: FightPlan, spawnProps: Properties) {
        val music = plan.music ?: return
        spawnProps.put(ConstKeys.MUSIC, music.name)
    }

    private fun standardBossPlan(music: MusicAsset?) = stageBossPlan(music, LevelVictory)

    private fun midBossPlan(music: MusicAsset?) =
        stageBossPlan(music, ExitMidBossFight, ResumeAfterMidBoss, AwaitBossGone)

    private fun stageBossPlan(music: MusicAsset?, vararg exit: Step<Fight>) = FightPlan(
        intro = gdxArrayOf(
            AwaitBossReady,
            HoldWorld,
            FillBossHealthBar,
            ReleaseWorld,
            ControllerOn
        ),
        win = FightRule.AllDefeated,
        outro = gdxArrayOf(
            BossDefeatLock,
            NotifyListenersOfBossDefeat,
            HideBossHealthBar,
            AwaitBossDefeatEnd,
            *exit
        ),
        music = music
    )
}
