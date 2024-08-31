package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.enemies.CactusLauncher.CactusLauncherState.*
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class CactusLauncher(game: MegamanMaverickGame): AbstractEnemy(game), IParentEntity, IAnimatedEntity {

    companion object {
        const val TAG = "CactusLauncher"
        private const val WAIT_DUR = 0.75f
        private const val FIRE_DUR = 0.5f
        private const val RELOAD_DUR = 0.5f
        private const val MAX_CHILDREN = 2
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class CactusLauncherState {
        WAIT, FIRE, RELOAD
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )
    override var children = Array<IGameEntity>()

    private val loop = Loop(CactusLauncherState.values().toGdxArray())
    private val timers = objectMapOf(
        "wait" to Timer(WAIT_DUR),
        "fire" to Timer(FIRE_DUR),
        "reload" to Timer(RELOAD_DUR)
    )

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("fire", atlas.findRegion("$TAG/fire"))
            regions.put("reload", atlas.findRegion("$TAG/reload"))
            regions.put("wait", atlas.findRegion("$TAG/wait"))
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        loop.reset()
        timers.values().forEach { it.reset() }
    }

    override fun onDestroy() {
        super.onDestroy()
        children.clear()
    }

    private fun launchMissile() {
        val missile = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.CACTUS_MISSILE)!!
        game.engine.spawn(missile, props(ConstKeys.POSITION to body.getTopCenterPoint()))
        children.add(missile)
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            val iter = children.iterator()
            while (iter.hasNext()) {
                val child = iter.next()
                if (child.dead) iter.remove()
            }

            if (children.size >= MAX_CHILDREN) {
                loop.setIndex(1)
                return@add
            }

            val key = when (loop.getCurrent()) {
                WAIT -> "wait"
                FIRE -> "fire"
                RELOAD -> "reload"
            }
            val timer = timers[key]
            timer.update(delta)
            if (timer.isFinished()) {
                timer.reset()
                loop.next()
                if (loop.getCurrent() == FIRE) launchMissile()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.35f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            val bodyPosition = body.getBottomCenterPoint()
            _sprite.setPosition(bodyPosition, Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (loop.getCurrent()) {
                WAIT -> "wait"
                FIRE -> "fire"
                RELOAD -> "reload"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "wait" to Animation(regions["wait"]),
            "fire" to Animation(regions["fire"], 2, 1, 0.1f, false),
            "reload" to Animation(regions["reload"], 2, 1, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}