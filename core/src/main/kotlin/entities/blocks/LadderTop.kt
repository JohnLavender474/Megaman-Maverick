package com.megaman.maverick.game.entities.blocks



import com.mega.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.FixtureLabel

com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.FixtureLabel

class LadderTop(game: MegamanMaverickGame) : Block(game) {

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.FRICTION_X, 0f)
        spawnProps.put(ConstKeys.FRICTION_Y, 0f)
        spawnProps.put(
            ConstKeys.FIXTURE_LABELS,
            objectSetOf(FixtureLabel.NO_PROJECTILE_COLLISION, FixtureLabel.NO_SIDE_TOUCHIE)
        )
        spawnProps.put(ConstKeys.BODY_LABELS, objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY))
        super.onSpawn(spawnProps)
    }
}
