package com.megaman.maverick.game.fights

import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.state.Step
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.entities.bosses.BigAssMaverickRobot
import com.megaman.maverick.game.entities.bosses.DesertMan
import com.megaman.maverick.game.entities.bosses.ElecDevil
import com.megaman.maverick.game.entities.bosses.GlacierMan
import com.megaman.maverick.game.entities.bosses.GutsTank
import com.megaman.maverick.game.entities.bosses.InfernoMan
import com.megaman.maverick.game.entities.bosses.MechaDragon
import com.megaman.maverick.game.entities.bosses.MoonHead
import com.megaman.maverick.game.entities.bosses.MoonMan
import com.megaman.maverick.game.entities.bosses.PenguinMiniBoss
import com.megaman.maverick.game.entities.bosses.PreciousWoman
import com.megaman.maverick.game.entities.bosses.ReactorMan
import com.megaman.maverick.game.entities.bosses.ReactorMonkey
import com.megaman.maverick.game.entities.bosses.RodentMan
import com.megaman.maverick.game.entities.bosses.SigmaRat
import com.megaman.maverick.game.entities.bosses.TimberWoman

object FightPlans {

    fun find(bossName: String, props: Properties): FightPlan? {
        val music = when (bossName) {
            BigAssMaverickRobot.TAG -> MusicAsset.VINNYZ_INTRO_BOSS_MUSIC_INTRO
            ElecDevil.TAG -> MusicAsset.JX_SHADOW_DEVIL_8BIT_REMIX_MUSIC
            GutsTank.TAG -> MusicAsset.MMX6_SIGMA_2_BATTLE_INTRO_MUSIC
            TimberWoman.TAG, MoonMan.TAG, ReactorMan.TAG,
            GlacierMan.TAG, DesertMan.TAG, InfernoMan.TAG, RodentMan.TAG, PreciousWoman.TAG,
            SigmaRat.TAG,
            MoonHead.TAG, MechaDragon.TAG, ReactorMonkey.TAG, PenguinMiniBoss.TAG -> null
            else -> return null
        }

        if (props.getOrDefault(ConstKeys.MINI, false, Boolean::class)) return miniBossPlan()

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

    private fun miniBossPlan() = FightPlan(
        intro = gdxArrayOf(
            AwaitBossReady,
            EndBossSpawn,
            ControllerOn
        ),
        win = FightRule.AllDefeated,
        outro = gdxArrayOf(
            NotifyListenersOfBossDefeat,
            AwaitBossDefeatEnd,
            ExitMidBossFight,
            ControllerOn,
            AwaitBossGone
        ),
        music = null
    )

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
