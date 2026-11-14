package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.AnimatorBuilder
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.pairTo
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset

class ProtoBuster(services: MegaServices) : Bullet(services) {

    companion object {
        const val TAG = "ProtoBuster"

        private var region: TextureRegion? = null
    }

    override fun init(vararg params: Any) {
        if (region == null)
            region = services.assets.getTextureAtlas(TextureAsset.PROTOMAN.source).findRegion(TAG)!!
        super.init()
    }

    override fun defineAnimationsComponent(): AnimationsComponent = AnimationsComponentBuilder(this)
        .key(Bullet.TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { TAG }
                .addAnimations(TAG pairTo Animation(region!!))
                .build()
        )
        .build()
}
