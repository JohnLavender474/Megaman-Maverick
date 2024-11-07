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
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.SmoothOscillationTimer
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.getCenter
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
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import java.util.*
import kotlin.reflect.KClass

class DemonMet(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "DemonMet"
        private const val STAND_DUR = 0.15f
        private const val FIRE_DELAY = 1.5f
        private const val FLY_SPEED = 4f
        private const val FIRE_PELLET_COUNT = 3
        private const val FIRE_PELLET_ANGLE_OFFSET = 25f
        private const val FIRE_SPEED = 10f
        private const val ANGEL_FLY_Y_IMPULSE = 10f
        private const val ANGEL_FLY_Y_MAX_SPEED = 10f
        private const val OSCILLATION_DUR = 2f
        private const val OSCILLATION_X = 1.25f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class DemonMetState { STAND, FLY, ANGEL }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(15),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
    )
    override lateinit var facing: Facing

    private val standTimer = Timer(STAND_DUR)
    private val fireTimer = Timer(FIRE_DELAY)
    private lateinit var state: DemonMetState
    private lateinit var target: Vector2
    private lateinit var xOscillation: SmoothOscillationTimer //  = SmoothOscillationTimer(OSCILLATION_DUR, min = -1f, max = 1f)
    private var targetReached = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            DemonMetState.entries.forEach {
                val key = it.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)

        val targets = PriorityQueue { o1: Vector2, o2: Vector2 ->
            val d1 = o1.dst2(getMegaman().body.getCenter())
            val d2 = o2.dst2(getMegaman().body.getCenter())
            d1.compareTo(d2)
        }
        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.TARGET) && value is RectangleMapObject)
                targets.add(value.rectangle.getCenter())
        }
        target = targets.poll()
        targetReached = false

        state = DemonMetState.STAND
        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT

        standTimer.reset()
        fireTimer.reset()

        xOscillation = SmoothOscillationTimer(OSCILLATION_DUR, -1f, 1f)
    }

    override fun onHealthDepleted() {
        body.physics.velocity.x = 0f
        state = DemonMetState.ANGEL
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (state) {
                DemonMetState.STAND -> {
                    facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT

                    standTimer.update(delta)
                    if (standTimer.isFinished()) state = DemonMetState.FLY
                }

                DemonMetState.FLY -> {
                    facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT

                    if (!targetReached && body.getCenter().epsilonEquals(target, 0.01f * ConstVals.PPM)) {
                        targetReached = true
                        target = body.getCenter()
                    }

                    if (targetReached) {
                        body.physics.velocity.y = 0f

                        xOscillation.update(delta)
                        body.physics.velocity.x = xOscillation.getValue() * OSCILLATION_X * ConstVals.PPM
                    } else {
                        val trajectory = target.cpy().sub(body.getCenter()).nor().scl(FLY_SPEED * ConstVals.PPM)
                        body.physics.velocity.set(trajectory)
                    }

                    fireTimer.update(delta)
                    if (fireTimer.isFinished()) {
                        fire()
                        fireTimer.reset()
                    }
                }

                DemonMetState.ANGEL -> {
                    if (body.physics.velocity.y < ANGEL_FLY_Y_MAX_SPEED * ConstVals.PPM)
                        body.physics.velocity.y += ANGEL_FLY_Y_IMPULSE * ConstVals.PPM * delta
                    else body.physics.velocity.y = ANGEL_FLY_Y_MAX_SPEED * ConstVals.PPM
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.85f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        val debugShapes = Array<() -> IDrawableShape?>()
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))
        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
        sprite.setSize(1.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { state.name.lowercase() }
        val animations = objectMapOf<String, IAnimation>(
            "stand" pairTo Animation(regions["stand"]),
            "fly" pairTo Animation(regions["fly"], 2, 2, 0.1f, true),
            "angel" pairTo Animation(regions["angel"], 2, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun fire() {
        for (i in 0 until FIRE_PELLET_COUNT) {
            val impulse = getMegaman().body.getCenter().sub(body.getCenter()).nor().scl(FIRE_SPEED * ConstVals.PPM)
            when (i) {
                0 -> impulse.rotateDeg(-FIRE_PELLET_ANGLE_OFFSET)
                2 -> impulse.rotateDeg(FIRE_PELLET_ANGLE_OFFSET)
            }
            val firePellet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.FIRE_PELLET)!!
            val spawn = body.getBottomCenterPoint().add(0.1f * ConstVals.PPM * facing.value, 0.1f * ConstVals.PPM)
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