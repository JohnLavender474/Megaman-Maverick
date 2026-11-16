package com.megaman.maverick.game.players

import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.ISizable
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.damage.IDamagerEntity
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.PropellerPlatform
import com.megaman.maverick.game.entities.blocks.RocketPlatform
import com.megaman.maverick.game.entities.bosses.BigAssMaverickRobot
import com.megaman.maverick.game.entities.bosses.BigAssMaverickRobotHand
import com.megaman.maverick.game.entities.bosses.D1000
import com.megaman.maverick.game.entities.bosses.D1000Core
import com.megaman.maverick.game.entities.bosses.DesertMan
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.enemies.HoverGunner
import com.megaman.maverick.game.entities.enemies.Junkroid
import com.megaman.maverick.game.entities.enemies.MetaDridler
import com.megaman.maverick.game.entities.enemies.MetaWheelF
import com.megaman.maverick.game.entities.explosions.BigAssMaverickRobotOrbExplosion
import com.megaman.maverick.game.entities.hazards.D1000CeilingDebris
import com.megaman.maverick.game.entities.hazards.Laser
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb
import com.megaman.maverick.game.entities.projectiles.D1000Bomb
import com.megaman.maverick.game.entities.projectiles.EnemyShot
import com.megaman.maverick.game.entities.projectiles.Needle
import com.megaman.maverick.game.entities.state.tags
import com.megaman.maverick.game.entities.utils.hardMode

// TODO: a player class cannot give its own entries yet; item 081 branch 081b adds an overrides parameter, with X as
//  its first caller
class PlayerDamageNegotiator(private val player: MegaGameEntity) : IDamageNegotiator {

    private val services = player.services

    private val custom = objectMapOf(
        BigAssMaverickRobotHand.TAG pairTo dmgNeg(1),
        BigAssMaverickRobot.TAG pairTo dmgNeg(1),
        BigAssMaverickRobotOrbExplosion.TAG pairTo dmgNeg(1),
        BigAssMaverickRobotOrb.TAG pairTo dmgNeg {
            val owner = (it as BigAssMaverickRobotOrb).owner
            if (owner is BigAssMaverickRobot || owner is BigAssMaverickRobotHand) 1 else 3
        },
        // TODO: consider varied damage per Desert Man damager (item 077)
        DesertMan.TAG pairTo dmgNeg(3),
        Needle.TAG pairTo dmgNeg(3),
        D1000.TAG pairTo dmgNeg(2),
        D1000Core.TAG pairTo dmgNeg(2),
        EnemyShot.TAG pairTo dmgNeg { (it as EnemyShot).kind.damage },
        Junkroid.TAG pairTo dmgNeg(4),
        HoverGunner.TAG pairTo dmgNeg(3),
        MetaWheelF.TAG pairTo dmgNeg(4),
        MetaDridler.TAG pairTo dmgNeg(8),
        D1000Bomb.TAG pairTo dmgNeg(2),
        D1000CeilingDebris.TAG pairTo dmgNeg(D1000CeilingDebris.DAMAGE),
        PropellerPlatform.TAG pairTo dmgNeg(1),
        RocketPlatform.TAG pairTo dmgNeg(1),
        Laser.TAG pairTo dmgNeg { if (services.gameState.hardMode) 15 else 8 },
    )
    private val entityTypes = objectSetOf(EntityType.ENEMY, EntityType.EXPLOSION)

    override fun get(damager: IDamagerEntity): Int {
        val entity = damager as MegaGameEntity
        if (entity is IOwnable<*> && entity.owner == player) return 0

        val tag = customTagOf(entity)
        return when {
            tag != null -> custom[tag].get(damager)
            entity is ISizable -> when (entity.size) {
                Size.LARGE -> 4
                Size.MEDIUM -> 3
                Size.SMALL -> 2
            }
            entity is IHazard -> entity.getDamageToMegaman()
            entityTypes.contains(entity.getType()) -> 3
            else -> 0
        }
    }

    private fun customTagOf(entity: MegaGameEntity): String? {
        val tags = entity.tags
        for (i in 0 until tags.size) if (custom.containsKey(tags[i])) return tags[i]
        return null
    }
}
