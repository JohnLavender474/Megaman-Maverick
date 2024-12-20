package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.AnimatedBlock
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntitySuppliers
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.getBounds
import java.util.*

class DisappearingBlocks(game: MegamanMaverickGame) : MegaGameEntity(game), IParentEntity, IAudioEntity {

    companion object {
        const val TAG = "DisappearingBlocks"
        private const val DEFAULT_DURATION = 1.15f
    }

    override var children = Array<IGameEntity>()

    private val keysToRender = LinkedList<String>()
    private val bounds = GameRectangle()
    private val timer = Timer()

    private lateinit var loop: Loop<String>

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(AudioComponent())
        addComponent(defineCullablesComponent())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ bounds }), debug = true))
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)

        val duration = spawnProps.getOrDefault(ConstKeys.DURATION, DEFAULT_DURATION, Float::class)
        GameLogger.debug(TAG, "onSpawn(): duration = $duration")
        timer.resetDuration(duration)

        val childrenPropsArray = convertObjectPropsToEntitySuppliers(spawnProps)
        childrenPropsArray.sort { o1, o2 ->
            val key1 = o1.second.get(ConstKeys.KEY, String::class)!!
            val key2 = o2.second.get(ConstKeys.KEY, String::class)!!
            key1.compareTo(key2)
        }

        GameLogger.debug(TAG, "onSpawn(): sorted childrenPropsArray = $childrenPropsArray")

        val keyArray = Array<String>()
        var currentKey: String? = null

        childrenPropsArray.forEach { (childSupplier, props) ->
            val child = childSupplier() as AnimatedBlock
            children.add(child)

            props.put(ConstKeys.RUN_ON_SPAWN, Runnable {
                child.body.physics.collisionOn = false
                child.body.forEachFixture { it.setActive(false) }
                child.hidden = true
            })

            child.spawn(props)

            val thisKey = props.get(ConstKeys.KEY, String::class)!!
            GameLogger.debug(TAG, "onSpawn(): thisKey = $thisKey")

            if (currentKey == null || currentKey != thisKey) {
                currentKey = thisKey
                keyArray.add(currentKey)

                GameLogger.debug(TAG, "onSpawn(): keyArray.add($currentKey)")
            }
        }

        loop = Loop(keyArray, true)
    }

    override fun onDestroy() {
        super.onDestroy()

        children.forEach { (it as MegaGameEntity).destroy() }
        children.clear()

        keysToRender.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        timer.update(it)
        if (timer.isFinished()) {
            val next = loop.next()

            GameLogger.debug(TAG, "defineUpdatablesComponent(): next = $next")

            keysToRender.add(next)
            if (keysToRender.size > 2) keysToRender.poll()

            GameLogger.debug(TAG, "defineUpdatablesComponent(): keysToRender = $keysToRender")

            var soundRequested = false
            children.forEach { block ->
                block as AnimatedBlock

                val blockKey = block.properties.get(ConstKeys.KEY, String::class)!!

                val on = keysToRender.contains(blockKey)

                block.body.physics.collisionOn = on
                block.body.forEachFixture { it.setActive(on) }
                block.hidden = !on

                if (on && blockKey == next) block.animators.values().forEach { it.reset() }

                val gameCamera = game.getGameCamera()
                if (!soundRequested && gameCamera.toGameRectangle().overlaps(block.body.getBounds())) {
                    requestToPlaySound(SoundAsset.DISAPPEARING_BLOCK_SOUND, false)
                    soundRequested = true
                }
            }

            timer.reset()
        }
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullablesComponent = CullablesComponent()
        val cullable = getGameCameraCullingLogic(game.getGameCamera(), { bounds })
        cullablesComponent.put(ConstKeys.CULL_OUT_OF_BOUNDS, cullable)
        return cullablesComponent
    }

    override fun getEntityType() = EntityType.SPECIAL

    override fun getTag(): String = TAG
}
