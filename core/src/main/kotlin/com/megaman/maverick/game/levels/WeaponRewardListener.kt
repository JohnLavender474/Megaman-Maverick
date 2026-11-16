package com.megaman.maverick.game.levels

import com.badlogic.gdx.utils.OrderedSet
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.players.PlayerSlot
import com.megaman.maverick.game.players.CharacterLoadouts
import com.megaman.maverick.game.players.MegaPlayer
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.state.IGameStateListener
import com.megaman.maverick.game.weapons.WeaponDef

class WeaponRewardListener(
    private val levelState: LevelState,
    private val players: MegaPlayers
) : IGameStateListener {

    companion object {
        const val TAG = "WeaponRewardListener"
    }

    override fun onAddLevelCompleted(level: LevelDefinition) {
        val slot0Player = players.get(PlayerSlot(0)) as? MegaPlayer
            ?: throw IllegalStateException("Slot 0 has no player to give the rewards of $level to")
        val character = slot0Player.character

        val levelRewards = CharacterLoadouts.of(character).rewardsFor(level)
        val rewards = if (levelRewards.isEmpty) null else OrderedSet<WeaponDef<*>>().apply {
            levelRewards.forEach { add(it) }
        }
        levelState.weaponsAttained = rewards
        GameLogger.debug(TAG, "onAddLevelCompleted(): level=$level, character=$character, rewards=$rewards")
    }
}
