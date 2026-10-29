package com.megaman.maverick.game.entities.blocks

import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.entities.decorations.BlockPiece.BlockPieceColor
import com.megaman.maverick.game.entities.decorations.WhiteBurst
import com.megaman.maverick.game.entities.hazards.Laser
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.setEntity
import com.megaman.maverick.game.world.body.setReceiver

class PinkBlock(game: MegaServices) : BreakableBlock(game), IDamageableEntity {

    override val invincible = false

    private var heartTankBlock = false

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.TYPE, PRECIOUS_TYPE)
        spawnProps.put(ConstKeys.COLOR, BlockPieceColor.PINK.name)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        heartTankBlock = spawnProps.getOrDefault(ConstKeys.HEART, false, Boolean::class)
    }

    override fun canBeDamagedBy(damager: IDamagerEntity) = damager is Laser && damager.owner.asPlayer != null

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        explodeAndDie()
        return true
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()
        val body = component.body

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle())
        damageableFixture.setEntity(this)
        body.addFixture(damageableFixture)

        blockFixture.setReceiver(Receivers.HIT_BY_EXPLOSION) {
            if (it is WhiteBurst &&
                it.owner is Laser &&
                (!heartTankBlock || (it.owner as Laser).owner.asPlayer != null)
            ) explodeAndDie()
        }

        return component
    }
}
