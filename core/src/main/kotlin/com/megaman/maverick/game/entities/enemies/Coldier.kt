package com.megaman.maverick.game.entities.enemies

import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.AbstractEnemy

/*
1: Puts his hand on his head and sends some snow flakes at Megaman and pushes him a bit (deals damage)

2: Raises his hand and sends a gust of cold air, pushing him with more intensity
 */
class Coldier(game: MegamanMaverickGame): AbstractEnemy(game), IAnimatedEntity, IFaceable {

    override lateinit var facing: Facing

    override fun init() {
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
    }

    override fun defineBodyComponent(): BodyComponent {
        TODO("Not yet implemented")
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder()
        .build()
}
