package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.damage.IDamager
import com.engine.drawables.sprites.SpritesComponent
import com.engine.world.BodyComponent
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import kotlin.reflect.KClass

class BabySpider(game: MegamanMaverickGame) : AbstractEnemy(game) {

    companion object {
        const val TAG = "BabySpider"
        private var region: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "BabySpider")
        super.init()
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        // TODO:
        //  spawn in air and fall straight down until land on ground
        //  at moment of landing, set x velocity towards Megaman
        //  walk in x direction on ground, scale wall up when side fixture touches wall
        //  walk on ceiling when ceiling reached, going left if right side was on wall or vice versa
    }

    override fun defineBodyComponent(): BodyComponent {
        TODO("Not yet implemented")
    }

    override fun defineSpritesComponent(): SpritesComponent {
        TODO("Not yet implemented")
    }
}