package com.megaman.maverick.game.entities.blocks

import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.decorations.BlockPiece.BlockPieceColor
import com.megaman.maverick.game.entities.decorations.WhiteBurst
import com.megaman.maverick.game.entities.hazards.Laser
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.setEntity
import com.megaman.maverick.game.world.body.setHitByExplosionReceiver

class PinkBlock(game: MegamanMaverickGame) : BreakableBlock(game), IDamageable {

    override val invincible = false

    private var heartTankBlock = false

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.TYPE, PRECIOUS_TYPE)
        spawnProps.put(ConstKeys.COLOR, BlockPieceColor.PINK.name)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        heartTankBlock = spawnProps.getOrDefault(ConstKeys.HEART, false, Boolean::class)
    }

    override fun canBeDamagedBy(damager: IDamager) = damager is Laser && damager.owner is Megaman

    override fun takeDamageFrom(damager: IDamager): Boolean {
        explodeAndDie()
        return true
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()
        val body = component.body

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle())
        damageableFixture.setEntity(this)
        body.addFixture(damageableFixture)

        blockFixture.setHitByExplosionReceiver {
            if (it is WhiteBurst &&
                it.owner is Laser &&
                (!heartTankBlock || (it.owner as Laser).owner is Megaman)
            ) explodeAndDie()
        }

        return component
    }
}
