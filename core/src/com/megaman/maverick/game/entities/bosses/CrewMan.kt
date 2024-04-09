package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.engine.common.enums.Facing
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.objects.Properties
import com.engine.damage.IDamager
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.world.BodyComponent
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import kotlin.reflect.KClass

class CrewMan(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "CrewMan"
        private var jumpRegion: TextureRegion? = null
        private var standRegion: TextureRegion? = null
        private var standHoldBlockRegion: TextureRegion? = null
        private var standMouthOpenRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()

    override lateinit var facing: Facing

    override fun init() {
        if (jumpRegion == null || standRegion == null || standHoldBlockRegion == null ||
            standMouthOpenRegion == null
        ) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            jumpRegion = atlas.findRegion("CrewMan/Jump")
            standRegion = atlas.findRegion("CrewMan/Stand")
            standHoldBlockRegion = atlas.findRegion("CrewMan/StandHoldBlock")
            standMouthOpenRegion = atlas.findRegion("CrewMan/StandMouthOpen")
        }
        super<AbstractBoss>.init()
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
    }

    override fun defineBodyComponent(): BodyComponent {
        TODO("Not yet implemented")
    }

    override fun defineSpritesComponent(): SpritesComponent {
        TODO("Not yet implemented")
    }

}