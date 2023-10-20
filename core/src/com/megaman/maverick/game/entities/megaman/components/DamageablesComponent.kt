package com.megaman.maverick.game.entities.megaman.components

import com.engine.common.extensions.objectMapOf
import com.engine.common.time.Timer
import com.engine.damage.Damageable
import com.engine.damage.DamageableComponent
import com.engine.damage.IDamager
import com.engine.entities.contracts.IBodyEntity
import com.engine.entities.contracts.IDamagerEntity
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.projectiles.Bullet
import kotlin.reflect.KClass

/*
private val dmgNegs: Map<Class<out IDamager?>, DamageNegotiation> =
    object : HashMap<Class<IDamager?>?, DamageNegotiation?>() {
      init {
        put(Bat::class.java, DamageNegotiation(2))
        put(Met::class.java, DamageNegotiation(2))
        put(Screwie::class.java, DamageNegotiation(1))
        put(Bullet::class.java, DamageNegotiation(2))
        put(Picket::class.java, DamageNegotiation(2))
        put(Ratton::class.java, DamageNegotiation(2))
        put(MagFly::class.java, DamageNegotiation(2))
        put(FlyBoy::class.java, DamageNegotiation(4))
        put(Penguin::class.java, DamageNegotiation(3))
        put(Snowball::class.java, DamageNegotiation(1))
        put(ChargedShot::class.java, DamageNegotiation(4))
        put(Fireball::class.java, DamageNegotiation(2))
        put(PreciousShot::class.java, DamageNegotiation(2))
        put(PreciousExplosion::class.java, DamageNegotiation(1))
        put(Dragonfly::class.java, DamageNegotiation(3))
        put(Matasaburo::class.java, DamageNegotiation(2))
        put(SniperJoe::class.java, DamageNegotiation(3))
        put(SpringHead::class.java, DamageNegotiation(2))
        put(FloatingCan::class.java, DamageNegotiation(2))
        put(LaserBeamer::class.java, DamageNegotiation(3))
        put(SuctionRoller::class.java, DamageNegotiation(2))
        put(ShieldAttacker::class.java, DamageNegotiation(2))
        put(GapingFish::class.java, DamageNegotiation(2))
      }
    }
 */

internal fun Megaman.defineDamageableComponent(): DamageableComponent {
  val damageNegotiations =
      objectMapOf<KClass<out IDamager>, Int>(
          Bullet::class to 2,
          // TODO: add more
      )

  val damageable =
      Damageable({ damager ->
        if (damager is IDamagerEntity && damageNegotiations.containsKey(damager::class)) {
          val damage = damageNegotiations[damager::class]
          getHealthPoints().translate(-damage)

          if (damager is IBodyEntity) {
            var bounceX = ConstVals.PPM * MegamanValues.DMG_X
            if (damager.body.x > body.x) bounceX *= -1f
            body.physics.velocity.x = bounceX
            body.physics.velocity.y = MegamanValues.DMG_Y * ConstVals.PPM
          }

          requestToPlaySound(SoundAsset.MEGAMAN_DAMAGE_SOUND.source, false)
          stopSound(SoundAsset.MEGA_BUSTER_CHARGING_SOUND.source)

          true
        } else false
      })

  return DamageableComponent(
      this,
      damageable,
      Timer(MegamanValues.DAMAGE_DURATION),
      Timer(MegamanValues.DAMAGE_RECOVERY_TIME))
}
