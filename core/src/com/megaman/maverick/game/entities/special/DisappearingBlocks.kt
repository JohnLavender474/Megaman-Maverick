package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.utils.Array
import com.engine.audio.AudioComponent
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.GameLogger
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.time.Timer
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.blocks.SpriteBlock
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntities

class DisappearingBlocks(game: MegamanMaverickGame) :
    GameEntity(game), IParentEntity, IAudioEntity {

  companion object {
    const val TAG = "DisappearingBlocks"
  }

  override val children = Array<SpriteBlock>()

  private lateinit var loop: Loop<String>
  private lateinit var timer: Timer

  override fun init() {
    addComponent(defineUpdatablesComponent())
    addComponent(AudioComponent(this))
  }

  override fun spawn(spawnProps: Properties) {
    super.spawn(spawnProps)

    val duration = spawnProps.getOrDefault(ConstKeys.DURATION, 1.5f, Float::class)
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
      children.add(child as SpriteBlock)
      game.gameEngine.spawn(child, props)

      val thisKey = props.get(ConstKeys.KEY, String::class)!!
      GameLogger.debug(TAG, "spawn(): thisKey = $thisKey")

      if (currentKey == null || currentKey != thisKey) {
        currentKey = thisKey
        keyArray.add(currentKey)
        GameLogger.debug(TAG, "spawn(): keyArray.add($currentKey)")
      }
    }

    loop = Loop(keyArray)
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
            val current = loop.getCurrent()

            children.forEach { spriteBlock ->
              val blockKey = spriteBlock.properties.get(ConstKeys.KEY, String::class)!!
              val on = current == blockKey

              spriteBlock.body.physics.collisionOn = on
              spriteBlock.body.fixtures.forEach { entry -> entry.second.active = on }
              spriteBlock.hidden = !on
            }

            timer.update(it)
            if (timer.isFinished()) {
              val next = loop.next()
              GameLogger.debug(TAG, "defineUpdatablesComponent(): next = $next")
              timer.reset()
              requestToPlaySound(SoundAsset.DISAPPEARING_BLOCK_SOUND, false)
            }
          })
}
