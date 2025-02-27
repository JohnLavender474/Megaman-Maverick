package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.SmoothOscillationTimer
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import java.util.*

class DemonMet(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "DemonMet"
        private const val STAND_DUR = 0.25f
        private const val FIRE_DELAY = 1.5f
        private const val FROZEN_DUR = 1f
        private const val FLY_SPEED = 5f
        private const val FIRE_PELLET_COUNT = 3
        private const val FIRE_PELLET_ANGLE_OFFSET = 5f
        private const val FIRE_SPEED = 10f
        private const val ANGEL_FLY_Y_IMPULSE = 10f
        private const val ANGEL_FLY_Y_MAX_SPEED = 10f
        private const val X_OSCILLATION_DUR = 2f
        private const val X_OSCILLATION = 1.75f
        private const val ALPHA_OSCILLATION_DUR = 0.5f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class DemonMetState { STAND, FLY, ANGEL, FROZEN }

    override lateinit var facing: Facing

    private val standTimer = Timer(STAND_DUR)
    private val fireTimer = Timer(FIRE_DELAY)
    private val frozenTimer = Timer(FROZEN_DUR)

    private val alphaOscillation = SmoothOscillationTimer(ALPHA_OSCILLATION_DUR, 0.5f, 0.75f)
    private val xOscillation = SmoothOscillationTimer(X_OSCILLATION_DUR, -1f, 1f)

    private lateinit var state: DemonMetState
    private lateinit var stateBeforeFrozen: DemonMetState

    private val target = Vector2()
    private val targetsPQ = PriorityQueue { o1: Vector2, o2: Vector2 ->
        val d1 = o1.dst2(megaman.body.getCenter())
        val d2 = o2.dst2(megaman.body.getCenter())
        d1.compareTo(d2)
    }
    private var targetReached = false
    private var exploded = false

    override fun init() {
        damageOverrides.put(Fireball::class, null)
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            DemonMetState.entries.forEach {
                val key = it.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
        damageOverrides.put(SmallIceCube::class, dmgNeg(15))
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.TARGET) && value is RectangleMapObject)
                targetsPQ.add(value.rectangle.getCenter())
        }
        target.set(targetsPQ.poll())
        targetsPQ.clear()
        targetReached = false

        state = DemonMetState.STAND
        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        standTimer.reset()
        fireTimer.reset()
        xOscillation.reset()
        alphaOscillation.reset()

        exploded = false
    }

    override fun onHealthDepleted() {
        body.physics.velocity.x = 0f
        state = DemonMetState.ANGEL
        if (!exploded) {
            explode()
            exploded = true
        }
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damaged = super.takeDamageFrom(damager)
        if (damaged && state != DemonMetState.FROZEN && damager is SmallIceCube) onFrozen()
        return damaged
    }

    private fun onFrozen() {
        stateBeforeFrozen = state
        state = DemonMetState.FROZEN

        frozenTimer.reset()

        requestToPlaySound(SoundAsset.ICE_SHARD_1_SOUND, false)
    }

    private fun explode() {
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.MAGMA_EXPLOSION)!!
        explosion.spawn(props(ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo body.getCenter()))
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (state != DemonMetState.ANGEL) {
                fireTimer.update(delta)
                if (fireTimer.isFinished()) {
                    fire()
                    fireTimer.reset()
                }
            }

            when (state) {
                DemonMetState.STAND -> {
                    facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

                    standTimer.update(delta)
                    if (standTimer.isFinished()) state = DemonMetState.FLY
                }

                DemonMetState.FLY -> {
                    facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

                    if (!targetReached && body.getCenter().epsilonEquals(target, 0.1f * ConstVals.PPM)) {
                        targetReached = true
                        target.set(body.getCenter())
                    }

                    if (targetReached) {
                        body.physics.velocity.y = 0f
                        xOscillation.update(delta)
                        body.physics.velocity.x = xOscillation.getValue() * X_OSCILLATION * ConstVals.PPM
                    } else {
                        val trajectory = GameObjectPools.fetch(Vector2::class)
                            .set(target)
                            .sub(body.getCenter())
                            .nor()
                            .scl(FLY_SPEED * ConstVals.PPM)
                        body.physics.velocity.set(trajectory)
                    }
                }

                DemonMetState.ANGEL -> {
                    if (body.physics.velocity.y < ANGEL_FLY_Y_MAX_SPEED * ConstVals.PPM)
                        body.physics.velocity.y += ANGEL_FLY_Y_IMPULSE * ConstVals.PPM * delta
                    else body.physics.velocity.y = ANGEL_FLY_Y_MAX_SPEED * ConstVals.PPM
                }

                DemonMetState.FROZEN -> {
                    body.physics.velocity.setZero()

                    frozenTimer.update(delta)
                    if (frozenTimer.isFinished()) {
                        state = stateBeforeFrozen

                        damageTimer.reset()

                        for (i in 0 until 5) {
                            val shard = MegaEntityFactory.fetch(IceShard::class)!!
                            shard.spawn(props(ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.INDEX pairTo i))
                        }
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.preProcess.put(ConstKeys.DEFAULT) {
            body.forEachFixture { it.setActive(state != DemonMetState.ANGEL) }
        }
        val debugShapes = Array<() -> IDrawableShape?>()
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))
        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { delta, _ ->
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink
            val alpha = if (state == DemonMetState.ANGEL) {
                alphaOscillation.update(delta)
                alphaOscillation.getValue()
            } else 1f
            sprite.setAlpha(alpha)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { state.name.lowercase() }
        val animations = objectMapOf<String, IAnimation>(
            "frozen" pairTo Animation(regions["frozen"]),
            "stand" pairTo Animation(regions["stand"]),
            "fly" pairTo Animation(regions["fly"], 2, 2, 0.1f, true),
            "angel" pairTo Animation(regions["angel"], 2, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun fire() {
        for (i in 0 until FIRE_PELLET_COUNT) {
            val impulse = megaman.body.getCenter().sub(body.getCenter()).nor().scl(FIRE_SPEED * ConstVals.PPM)
            when (i) {
                0 -> impulse.rotateDeg(-FIRE_PELLET_ANGLE_OFFSET)
                2 -> impulse.rotateDeg(FIRE_PELLET_ANGLE_OFFSET)
            }

            val spawn = body.getPositionPoint(Position.BOTTOM_CENTER)
                .add(0.1f * ConstVals.PPM * facing.value, 0.25f * ConstVals.PPM)

            val firePellet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.FIRE_PELLET)!!
            firePellet.spawn(
                props(
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.IMPULSE pairTo impulse
                )
            )
        }

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.WHIP_SOUND, false)
    }
}
