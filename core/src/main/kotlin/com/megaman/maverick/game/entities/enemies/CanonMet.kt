package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.AbstractEnemy

class CanonMet(game: MegamanMaverickGame): AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "CanonMet"
        private val regions = ObjectMap<String, TextureRegion>()
    }

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
