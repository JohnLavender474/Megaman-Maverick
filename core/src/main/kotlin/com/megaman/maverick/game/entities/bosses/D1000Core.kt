package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.entities.utils.X6Frames
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.X6Shot
import com.megaman.maverick.game.entities.projectiles.X6Shot.X6ShotType
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class D1000Core(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IDamageableEntity {

    companion object {
        const val TAG = "D1000Core"

        private const val SIZE = 0.625f * ConstVals.PPM
        private const val SHOT_SPEED = 3.75f * ConstVals.PPM
        private val SHOT_TIMES = floatArrayOf(0f, 0.13f, 0.27f, 0.4f)
        private const val SUMMON_SIGNAL_DELAY = 2.05f
        private const val SIGNAL_DELAY = 1.65f
        private const val MOVE_DELAY = 1f
    }

    private enum class D1000CoreState { APPEAR, IDLE, SUMMON, HIT, AWAY }

    override val invincible: Boolean
        get() = !vulnerable

    var vulnerable = false

    private var owner: D1000? = null
    private val position = Vector2()
    private val other = Vector2()

    private var state = D1000CoreState.APPEAR
    private var time = 0f
    private var signalPending = false
    private var shotsFired = 0
    private var hidden = false

    private val animations = ObjectMap<String, IAnimation>()
    private lateinit var animator: Animator

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source)
        X6Frames.animations(atlas, TAG, D1000Frames.CORE, animations)
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, D1000::class)!!
        position.set(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
        other.set(spawnProps.get(ConstKeys.OTHER, Vector2::class)!!)
        body.setCenter(position)
        vulnerable = false
        hidden = false
        appear()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        owner = null
    }

    fun summon() {
        GameLogger.debug(TAG, "summon()")
        setState(D1000CoreState.SUMMON)
        shotsFired = 0
    }

    override fun canBeDamagedBy(damager: IDamagerEntity) =
        vulnerable && !hidden && damager is ChargedShot && damager.fullyCharged

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        if (!canBeDamagedBy(damager)) return false
        GameLogger.debug(TAG, "takeDamageFrom(): damager=$damager")
        vulnerable = false
        setState(D1000CoreState.HIT)
        services.audio.playSound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        owner?.stun()
        return true
    }

    private fun setState(next: D1000CoreState) {
        state = next
        time = 0f
    }

    private fun appear() {
        signalPending = true
        setState(D1000CoreState.APPEAR)
        services.audio.playSound(SoundAsset.ELECTRIC_1_SOUND, false)
    }

    private fun signal() {
        owner?.onCoreSignal()
    }

    private fun shoot() {
        val trajectory = targetPlayer.body.getCenter().sub(position).nor()
            .scl(SHOT_SPEED)
        val shot = MegaEntityFactory.fetch(X6Shot::class)!!
        shot.spawn(
            props(
                ConstKeys.OWNER pairTo owner,
                ConstKeys.TYPE pairTo X6ShotType.CORE,
                ConstKeys.POSITION pairTo position.cpy(),
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )
        services.audio.playSound(SoundAsset.BLAST_2_SOUND, false)
    }

    private fun currentAnimation() = animator.currentAnimation

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        time += delta
        when (state) {
            D1000CoreState.APPEAR -> if (currentAnimation()?.isFinished() == true && animator.currentKey == "core_appear")
                setState(D1000CoreState.IDLE)

            D1000CoreState.IDLE -> if (signalPending && time >= SIGNAL_DELAY) {
                signalPending = false
                signal()
            }

            D1000CoreState.SUMMON -> {
                if (shotsFired < SHOT_TIMES.size && time >= SHOT_TIMES[shotsFired]) {
                    shoot()
                    shotsFired++
                }
                if (time >= SUMMON_SIGNAL_DELAY) {
                    setState(D1000CoreState.IDLE)
                    signal()
                }
            }

            D1000CoreState.HIT -> if (currentAnimation()?.isFinished() == true && animator.currentKey == "core_vanish") {
                hidden = true
                setState(D1000CoreState.AWAY)
            }

            D1000CoreState.AWAY -> if (time >= MOVE_DELAY) {
                val swap = position.cpy()
                position.set(other)
                other.set(swap)
                body.setCenter(position)
                hidden = false
                appear()
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(SIZE)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val damageable = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(SIZE))
        body.addFixture(damageable)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.setZero()
            damageable.setActive(!hidden)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 2))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val key = animator.currentKey ?: "core"
            X6Frames.place(sprite, D1000Frames.CORE[key], position.x, position.y, false)
            sprite.hidden = hidden
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when (state) {
                D1000CoreState.APPEAR -> "core_appear"
                D1000CoreState.HIT, D1000CoreState.AWAY -> "core_vanish"
                else -> "core"
            }
        }
        animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.BOSS

    override fun getTag() = TAG
}
