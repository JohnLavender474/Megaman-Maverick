package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.contracts.AbstractProjectile

// Similar to what happened with `PreciousGemCluster`, I wrote the `PreciousGem` class too tightly coupled to Precious
// Woman and didn't design the class to work well with Megaman. Rather than refactor that class to be "proper", instead
// I'll be lazy and create this new class to be used by Megaman.
class MegaPreciousGem(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "MegaPreciousGem"
        private const val CULL_TIME = 5f
        private const val SIZE_INCR_DELAY_DUR = 0.1f
        private val BODY_SIZES = gdxArrayOf(0.25f, 0.5f, 0.75f, 1f)
        private val regions = ObjectMap<PreciousGemColor, TextureRegion>()
    }

    enum class PreciousGemColor { PURPLE, BLUE, PINK }

    var targetReached = false
        private set

    private var stateIndex = 0

    private val sizeIncreaseDelay = Timer(SIZE_INCR_DELAY_DUR)
    private var sizeIncreaseIndex = 0

    private val target = Vector2()
    private var speed = 0f

    private lateinit var color: PreciousGemColor

    override fun init() {
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun defineBodyComponent(): BodyComponent {
        TODO("Not yet implemented")
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .build()
}
