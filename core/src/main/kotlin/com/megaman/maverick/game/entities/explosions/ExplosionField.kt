package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory

import com.megaman.maverick.game.utils.GameObjectPools

class ExplosionField(game: MegamanMaverickGame): MegaGameEntity(game), IOwnable {

    companion object {
        const val TAG = "ExplosionField"
        private const val DEFAULT_DUR = 0.3f
        private const val DEFAULT_FREQUENCY = 0.15f
        private const val DEFAULT_EXPLODE_ON_START = true
    }

    override var owner: GameEntity? = null

    private lateinit var bounds: GameRectangle
    private lateinit var timer: Timer
    private lateinit var frequency: Timer

    private var onFinished: (() -> Unit)? = null
    private var explodeOnStart = true
    private var started = false

    override fun init() {
        super.init()
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps = $spawnProps")
        super.onSpawn(spawnProps)

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class) ?: this

        val dur = spawnProps.getOrDefault(ConstKeys.DURATION, DEFAULT_DUR, Float::class)
        timer = Timer(dur)

        val freq = spawnProps.getOrDefault(ConstKeys.FREQUENCY, DEFAULT_FREQUENCY, Float::class)
        frequency = Timer(freq)

        explodeOnStart = spawnProps.getOrDefault(ConstKeys.START, DEFAULT_EXPLODE_ON_START, Boolean::class)
        onFinished = spawnProps.get(ConstKeys.END) as (() -> Unit)?

        started = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        onFinished?.invoke()
    }

    private fun spawnExplosion() {
        GameLogger.debug(TAG, "spawnExplosion()")
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
        explosion.spawn(props(
            ConstKeys.POSITION pairTo bounds.getRandomPositionInBounds(GameObjectPools.fetch(Vector2::class)),
            ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND,
            ConstKeys.OWNER pairTo owner
        ))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (explodeOnStart && !started) spawnExplosion()
        started = true

        frequency.update(delta)
        if (frequency.isFinished()) {
            spawnExplosion()
            frequency.reset()
        }

        timer.update(delta)
        if (timer.isFinished()) {
            destroy()
            return@UpdatablesComponent
        }
    })

    override fun getEntityType() = EntityType.EXPLOSION

    override fun getTag() = TAG
}
