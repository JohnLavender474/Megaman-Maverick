package com.megaman.maverick.game.fights

import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.entities.bosses.BigAssMaverickRobot

object FightPlans {

    fun find(bossName: String): FightPlan? = when (bossName) {
        BigAssMaverickRobot.TAG -> stageBossPlan(MusicAsset.VINNYZ_INTRO_BOSS_MUSIC_INTRO)
        else -> null
    }

    fun resolve(plan: FightPlan, spawnProps: Properties) {
        spawnProps.put(ConstKeys.MUSIC, plan.music.name)
    }

    private fun stageBossPlan(music: MusicAsset) = FightPlan(
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
            LevelVictory
        ),
        music = music
    )
}
