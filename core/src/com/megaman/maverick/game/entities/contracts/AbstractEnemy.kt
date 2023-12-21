package com.megaman.maverick.game.entities.contracts

import com.badlogic.gdx.utils.ObjectMap
import com.engine.audio.AudioComponent
import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.common.extensions.objectSetOf
import com.engine.common.getRandom
import com.engine.common.objects.props
import com.engine.common.time.Timer
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullablesComponent
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.sprites.SpriteComponent
import com.engine.entities.GameEntity
import com.engine.entities.contracts.IAudioEntity
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.ICullableEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.points.PointsComponent
import com.engine.updatables.UpdatablesComponent
import com.engine.world.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ItemsFactory
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import kotlin.reflect.KClass

abstract class AbstractEnemy(game: MegamanMaverickGame, private val cullTime: Float = 1f) :
    GameEntity(game),
    IDamager,
    IDamageable,
    IBodyEntity,
    IAudioEntity,
    IHealthEntity,
    ISpriteEntity,
    ICullableEntity {

  companion object {
    const val TAG = "AbstractEnemy"
    private const val DEFAULT_DMG_DURATION = .15f
    private const val DEFAULT_DMG_BLINK_DUR = .025f
  }

  override val invincible: Boolean
    get() = !dmgTimer.isFinished()

  protected abstract val damageNegotiations: ObjectMap<KClass<out IDamager>, Int>

  protected val dmgTimer = Timer(DEFAULT_DMG_DURATION)
  protected val dmgBlinkTimer = Timer(DEFAULT_DMG_BLINK_DUR)
  protected var dmgBlink = false
  protected var dropItemOnDeath = true

  override fun init() {
    addComponent(definePointsComponent())
    addComponent(defineBodyComponent())
    addComponent(defineSpriteComponent())
    addComponent(AudioComponent(this))

    val cullablesComponent = CullablesComponent(this)
    defineCullablesComponent(cullablesComponent)
    addComponent(cullablesComponent)

    val updatablesComponent = UpdatablesComponent(this)
    defineUpdatablesComponent(updatablesComponent)
    addComponent(updatablesComponent)

    runnablesOnDestroy.add {
      if (getCurrentHealth() == 0) {
        disintegrate()
        if (dropItemOnDeath) {
          val randomInt = getRandom(0, 10)
          val entity =
              when (randomInt) {
                0 -> EntityFactories.fetch(EntityType.ITEM, ItemsFactory.HEALTH_BULB)
                // TODO: add more items
                else -> null
              }
          entity?.let { game.gameEngine.spawn(it, props(ConstKeys.POSITION to body.getCenter())) }
        }
      }
    }
  }

  protected open fun definePointsComponent(): PointsComponent {
    val pointsComponent = PointsComponent(this)
    pointsComponent.putPoints(ConstKeys.HEALTH, ConstVals.MAX_HEALTH)
    return pointsComponent
  }

  protected abstract fun defineBodyComponent(): BodyComponent

  protected abstract fun defineSpriteComponent(): SpriteComponent

  override fun canBeDamagedBy(damager: IDamager) =
      !invincible && damageNegotiations.containsKey(damager::class)

  override fun takeDamageFrom(damager: IDamager): Boolean {
    val damagerKey = damager::class
    if (!damageNegotiations.containsKey(damagerKey)) return false

    val damage = damageNegotiations[damagerKey]
    getHealthPoints().translate(-damage)
    requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
    return true
  }

  override fun canDamage(damageable: IDamageable) = true

  override fun onDamageInflictedTo(damageable: IDamageable) {
    // do nothing
  }

  protected open fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
    updatablesComponent.add {
      dmgTimer.update(it)
      if (!dmgTimer.isFinished()) {
        dmgBlinkTimer.update(it)
        if (dmgBlinkTimer.isFinished()) {
          dmgBlinkTimer.reset()
          dmgBlink = !dmgBlink
        }
      }
      if (dmgTimer.isJustFinished()) dmgBlink = false
    }
  }

  protected open fun defineCullablesComponent(cullablesComponent: CullablesComponent) {
    val cullOnOutOfBounds = getGameCameraCullingLogic(this, cullTime)
    cullablesComponent.add(cullOnOutOfBounds)

    val eventsToCullOn =
        objectSetOf<Any>(
            EventType.GAME_OVER,
            EventType.PLAYER_SPAWN,
            EventType.BEGIN_ROOM_TRANS,
            EventType.GATE_INIT_OPENING)
    val cullOnEvents =
        CullableOnEvent(
            {
              GameLogger.debug(TAG, "Checking if event is to trigger cull = $it")
              eventsToCullOn.contains(it.key)
            },
            eventsToCullOn)
    runnablesOnSpawn.add {
      game.eventsMan.addListener(cullOnEvents)
      GameLogger.debug(TAG, "Added CullableOnEvent from EventsManager")
    }
    runnablesOnDestroy.add {
      game.eventsMan.removeListener(cullOnEvents)
      GameLogger.debug(TAG, "Removed CullableOnEvent from EventsManager")
    }

    cullablesComponent.add(cullOnEvents)
  }

  protected open fun disintegrate() {
    getMegamanMaverickGame().audioMan.playSound(SoundAsset.ENEMY_DAMAGE_SOUND)
    val disintegration =
        EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.DISINTEGRATION)
    game.gameEngine.spawn(disintegration!!, props(ConstKeys.POSITION to body.getCenter()))
  }

  protected open fun explode() {
    getMegamanMaverickGame().audioMan.playSound(SoundAsset.ENEMY_DAMAGE_SOUND)
    val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)
    game.gameEngine.spawn(explosion!!, props(ConstKeys.POSITION to body.getCenter()))
  }

  fun isMegamanShootingAtMe(): Boolean {
    val megaman = getMegamanMaverickGame().megaman
    if (!megaman.shooting) return false

    return body.x < megaman.body.x && megaman.facing == Facing.LEFT ||
        body.x > megaman.body.x && megaman.facing == Facing.RIGHT
  }
}
