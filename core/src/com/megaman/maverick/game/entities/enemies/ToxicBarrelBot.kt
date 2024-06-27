package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.AnimationsComponent
import com.engine.common.enums.Facing
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamager
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.world.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import kotlin.reflect.KClass

class ToxicBarrelBot(game: MegamanMaverickGame): AbstractEnemy(game), IAnimatedEntity, IFaceable {

    enum class ToxicBarrelBotState {
        CLOSED,
        OPENING_TOP,
        OPEN_TOP,
        CLOSING_TOP,
        OPENING_CENTER,
        OPEN_CENTER,
        CLOSING_CENTER
    }

    companion object {
        const val TAG = "ToxicBarrelBot"
        private var closedRegion: TextureRegion? = null
        private var openCenterRegion: TextureRegion? = null
        private var openTopRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    override lateinit var facing: Facing

    private lateinit var state: ToxicBarrelBotState

    override fun init() {
        if (closedRegion == null || openCenterRegion == null || openTopRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            closedRegion = atlas.findRegion("ToxicBarrelBot/Closed")
            openCenterRegion = atlas.findRegion("ToxicBarrelBot/OpenCenter")
            openTopRegion = atlas.findRegion("ToxicBarrelBot/OpenTop")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
    }

    override fun defineBodyComponent(): BodyComponent {
        TODO("Not yet implemented")
    }

    override fun defineSpritesComponent(): SpritesComponent {
        TODO("Not yet implemented")
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        TODO()
    }
}