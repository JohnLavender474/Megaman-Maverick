package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.AnimationsComponent
import com.engine.common.enums.Direction
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.toGdxArray
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.time.Timer
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.world.BodyComponent
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IHazard

class LavaBeamer(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IHazard, IDirectionRotatable {

    companion object {
        const val TAG = "LavaBeamer"
        private const val IDLE_DUR = 1f
        private const val SWITCHING_ON_DUR = 0.5f
        private const val FIRING_DUR = 0.25f
        private const val FIRE_SPEED = 6f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class LavaBeamerState {
        IDLE,
        SWITCHING_ON,
        FIRING
    }

    override var directionRotation: Direction?
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    private val loop = Loop(LavaBeamerState.values().toGdxArray())
    private val idleTimer = Timer(IDLE_DUR)
    private val switchingOnTimer = Timer(SWITCHING_ON_DUR)
    private val firingTimer = Timer(FIRING_DUR)

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)

        }
        super<MegaGameEntity>.init()
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

    }

    private fun defineBodyComponent(): BodyComponent {
        TODO()
    }

    private fun defineSpritesComponent(): SpritesComponent {
        TODO()
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        TODO()
    }
}