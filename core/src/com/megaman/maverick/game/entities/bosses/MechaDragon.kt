package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.AnimationsComponent
import com.engine.common.enums.Facing
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.interfaces.IFaceable
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.world.BodyComponent
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import kotlin.reflect.KClass

class MechaDragon(game: MegamanMaverickGame): AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "MechaDragon"
        private const val IDLE_DUR = 1.5f
        private const val FIRE_DELAY = 0.25f
        private const val FIRES_TO_SHOOT = 3
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class MechaDragonState {
        RETURNING, IDLE, FIRING, CHARGING
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    override lateinit var facing: Facing

    private val loop = Loop(MechaDragonState.values().toGdxArray())
    private val idleTimer = Timer(IDLE_DUR)
    private val fireDelayTimer = Timer(FIRE_DELAY)
    private lateinit var spot1: Vector2
    private lateinit var spot2: Vector2
    private var index = 0
    private var chargeTopY = 0f
    private var chargeBottomY = 0f
    private var firesShot = 0

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            regions.put("fly", atlas.findRegion("$TAG/Fly"))
            regions.put("shoot", atlas.findRegion("$TAG/Shoot"))
        }
        super<AbstractBoss>.init()
        addComponent(defineAnimationsComponent())
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

    private fun defineAnimationsComponent(): AnimationsComponent {
        TODO()
    }
}