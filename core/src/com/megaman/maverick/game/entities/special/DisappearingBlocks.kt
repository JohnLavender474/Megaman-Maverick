package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.utils.Array
import com.engine.audio.AudioComponent
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.GameLogger
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.cullables.CullablesComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.blocks.AnimatedBlock
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntities
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import java.util.*

class DisappearingBlocks(game: MegamanMaverickGame) :
    GameEntity(game), IParentEntity, IAudioEntity {

    companion object {
        const val TAG = "DisappearingBlocks"
        private const val DEFAULT_DURATION = 1.15f
    }

    override var children = Array<IGameEntity>()

    private val keysToRender = LinkedList<String>()

    private lateinit var bounds: GameRectangle
    private lateinit var loop: Loop<String>
    private lateinit var timer: Timer

    override fun init() {
        addComponent(defineUpdatablesComponent())
        addComponent(AudioComponent(this))
        addComponent(defineCullablesComponent())
        addComponent(
            DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ bounds }), debug = true)
        )
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!

        val duration = spawnProps.getOrDefault(ConstKeys.DURATION, DEFAULT_DURATION, Float::class)
        GameLogger.debug(TAG, "spawn(): duration = $duration")
        timer = Timer(duration)

        val childrenPropsArray = convertObjectPropsToEntities(spawnProps)
        childrenPropsArray.sort { o1, o2 ->
            val name1 = o1.second.get(ConstKeys.KEY, String::class)!!
            val name2 = o2.second.get(ConstKeys.KEY, String::class)!!
            name1.compareTo(name2)
        }
        GameLogger.debug(TAG, "spawn(): sorted childrenPropsArray = $childrenPropsArray")

        val keyArray = Array<String>()
        var currentKey: String? = null

        childrenPropsArray.forEach { (child, props) ->
            child as AnimatedBlock
            children.add(child)

            props.put(
                ConstKeys.RUN_ON_SPAWN,
                Runnable {
                    child.body.physics.collisionOn = false
                    child.body.fixtures.forEach { entry -> entry.second.active = false }
                    child.hidden = true
                })
            game.gameEngine.spawn(child, props)

            val thisKey = props.get(ConstKeys.KEY, String::class)!!
            GameLogger.debug(TAG, "spawn(): thisKey = $thisKey")

            if (currentKey == null || currentKey != thisKey) {
                currentKey = thisKey
                keyArray.add(currentKey)
                GameLogger.debug(TAG, "spawn(): keyArray.add($currentKey)")
            }
        }

        loop = Loop(keyArray, true)
    }

    override fun onDestroy() {
        super<GameEntity>.onDestroy()
        children.forEach { it.kill(props(CAUSE_OF_DEATH_MESSAGE to "Culled by parent")) }
        children.clear()
    }

    private fun defineUpdatablesComponent() =
        UpdatablesComponent(
            this,
            {
                timer.update(it)
                if (timer.isFinished()) {
                    val next = loop.next()
                    GameLogger.debug(TAG, "defineUpdatablesComponent(): next = $next")

                    keysToRender.add(next)
                    if (keysToRender.size > 2) keysToRender.poll()
                    GameLogger.debug(TAG, "defineUpdatablesComponent(): keysToRender = $keysToRender")

                    children.forEach { spriteBlock ->
                        spriteBlock as AnimatedBlock

                        val blockKey = spriteBlock.properties.get(ConstKeys.KEY, String::class)!!
                        val on = keysToRender.contains(blockKey)

                        if (blockKey == next) spriteBlock.reset()
                        spriteBlock.body.physics.collisionOn = on
                        spriteBlock.body.fixtures.forEach { entry -> entry.second.active = on }
                        spriteBlock.hidden = !on
                    }

                    requestToPlaySound(SoundAsset.DISAPPEARING_BLOCK_SOUND, false)
                    timer.reset()
                }
            })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullablesComponent = CullablesComponent(this)
        val cullable =
            getGameCameraCullingLogic((game as MegamanMaverickGame).getGameCamera(), { bounds })
        cullablesComponent.put(ConstKeys.CULL_OUT_OF_BOUNDS, cullable)
        return cullablesComponent
    }
}
