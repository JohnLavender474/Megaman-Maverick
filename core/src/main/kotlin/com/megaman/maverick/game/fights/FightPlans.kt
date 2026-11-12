package com.megaman.maverick.game.fights

import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.state.Step
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.entities.bosses.BigAssMaverickRobot
import com.megaman.maverick.game.entities.bosses.D1000
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
import com.megaman.maverick.game.entities.bosses.WilyFinalBoss

object FightPlans {

    fun find(bossName: String, props: Properties): FightPlan {
        val music = when (bossName) {
            BigAssMaverickRobot.TAG -> MusicAsset.VINNYZ_INTRO_BOSS_MUSIC_INTRO
            D1000.TAG -> MusicAsset.MMX6_D1000_MUSIC
            ElecDevil.TAG -> MusicAsset.JX_SHADOW_DEVIL_8BIT_REMIX_MUSIC
            GutsTank.TAG -> MusicAsset.MMX6_SIGMA_2_BATTLE_INTRO_MUSIC
            TimberWoman.TAG, MoonMan.TAG, ReactorMan.TAG,
            GlacierMan.TAG, DesertMan.TAG, InfernoMan.TAG, RodentMan.TAG, PreciousWoman.TAG,
            SigmaRat.TAG,
            MoonHead.TAG, MechaDragon.TAG, ReactorMonkey.TAG, PenguinMiniBoss.TAG -> null
            WilyFinalBoss.TAG -> return finalBossPlan()
            else -> throw MissingFightPlanException(bossName)
        }

        val gateKey = props.get("${ConstKeys.BOSS}_${ConstKeys.KEY}", String::class)

        if (props.getOrDefault(ConstKeys.MINI, false, Boolean::class)) return miniBossPlan(gateKey)

        val end = props.getOrDefault(ConstKeys.END, true, Boolean::class)
        return if (end) standardBossPlan(music, gateKey) else midBossPlan(music, gateKey)
    }

    fun resolve(plan: FightPlan, spawnProps: Properties) {
        val music = plan.music ?: return
        spawnProps.put(ConstKeys.MUSIC, music.name)
    }

    private fun standardBossPlan(music: MusicAsset?, gateKey: String?) = stageBossPlan(music, gateKey, LevelVictory)

    private fun finalBossPlan() = stageBossPlan(MusicAsset.OUR_BLOOD_BOILS_8_BIT, null)

    private fun midBossPlan(music: MusicAsset?, gateKey: String?) =
        stageBossPlan(music, gateKey, UnlockGate, ResumeAfterMidBoss, AwaitBossGone)

    private fun miniBossPlan(gateKey: String?) = FightPlan(
        intro = gdxArrayOf(
            AwaitBossReady,
            EndBossSpawn,
            StartBossFight,
            ControllerOn
        ),
        win = FightRule.AllDefeated,
        outro = gdxArrayOf(
            NotifyListenersOfBossDefeat,
            AwaitBossDefeatEnd,
            UnlockGate,
            ControllerOn,
            AwaitBossGone
        ),
        music = null,
        gateKey = gateKey
    )

    private fun stageBossPlan(music: MusicAsset?, gateKey: String?, vararg exit: Step<Fight>) = FightPlan(
        intro = gdxArrayOf(
            AwaitBossReady,
            HoldWorld,
            FillBossHealthBar,
            StartBossFight,
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
        music = music,
        gateKey = gateKey
    )
}
