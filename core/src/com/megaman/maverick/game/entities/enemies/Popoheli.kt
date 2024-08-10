package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimator
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.UpdateFunction
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.getCenter
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.*
import com.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.overlapsGameCamera
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import java.util.*
import kotlin.reflect.KClass

class Popoheli(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "Popoheli"
        private const val SPEED = 10f
        private const val ATTACK_DELAY = 0.25f
        private const val ATTACK_DUR = 1f
        private const val FLAMES = 4
        private const val FLAME_PADDING = 0.35f
        private var heliRegion: TextureRegion? = null
        private var flameRegion: TextureRegion? = null
    }

    enum class PopoheliState { APPROACHING, ATTACKING, FLEEING }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(15),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }, ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }
    )
    override lateinit var facing: Facing

    private val attackDelayTimer = Timer(ATTACK_DELAY)
    private val attackTimer = Timer(ATTACK_DUR)
    private val attacking: Boolean
        get() = state == PopoheliState.ATTACKING && attackDelayTimer.isFinished()

    private lateinit var state: PopoheliState
    private lateinit var target: Vector2

    override fun init() {
        if (heliRegion == null || flameRegion == null) {
            heliRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, "Popoheli")
            flameRegion = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, "Flame2")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val megamanCenter = getMegaman().body.getCenter()
        val targets = PriorityQueue<Vector2> { target1, target2 ->
            target1.dst2(megamanCenter).compareTo(target2.dst2(megamanCenter))
        }
        spawnProps.getAllMatching { it.toString().startsWith(ConstKeys.TARGET) }
            .forEach { targets.add((it.second as RectangleMapObject).rectangle.getCenter()) }
        target = targets.poll()

        state = PopoheliState.APPROACHING
        facing = if (target.x < body.x) Facing.LEFT else Facing.RIGHT

        attackTimer.reset()
        attackDelayTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (state) {
                PopoheliState.APPROACHING -> {
                    val trajectory = target.cpy().sub(body.getCenter()).nor().scl(SPEED * ConstVals.PPM)
                    body.physics.velocity = trajectory

                    if (body.getCenter().epsilonEquals(target, 0.1f * ConstVals.PPM)) {
                        body.physics.velocity.setZero()
                        state = PopoheliState.ATTACKING
                    }
                }

                PopoheliState.ATTACKING -> {
                    attackDelayTimer.update(delta)
                    if (!attackDelayTimer.isFinished()) return@add
                    if (attackDelayTimer.isJustFinished() && overlapsGameCamera()) requestToPlaySound(
                        SoundAsset.ATOMIC_FIRE_SOUND,
                        false
                    )

                    attackTimer.update(delta)
                    if (attackTimer.isFinished()) state = PopoheliState.FLEEING
                }

                PopoheliState.FLEEING -> {
                    body.physics.velocity.y = SPEED * ConstVals.PPM
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.65f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.GRAY
        // debugShapes.add { bodyFixture.getShape() }

        val damagerFixture1 = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture1)
        damagerFixture1.rawShape.color = Color.RED
        // debugShapes.add { damagerFixture1.getShape() }

        val damagerFixture2 = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(
                FLAMES * FLAME_PADDING * ConstVals.PPM, 0.2f * ConstVals.PPM
            )
        )
        body.addFixture(damagerFixture2)
        debugShapes.add { damagerFixture2.getShape() }

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(
                0.25f * ConstVals.PPM, 0.5f * ConstVals.PPM
            )
        )
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(
                0.35f * ConstVals.PPM, 0.65f * ConstVals.PPM
            )
        )
        body.addFixture(shieldFixture)
        shieldFixture.rawShape.color = Color.GREEN
        debugShapes.add { shieldFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            damagerFixture2.offsetFromBodyCenter = Vector2(
                FLAMES * FLAME_PADDING * facing.value * ConstVals.PPM / 2f, -0.5f * ConstVals.PPM
            )
            damagerFixture2.active = attacking
            damagerFixture2.rawShape.color = if (damagerFixture2.active) Color.GRAY else Color.RED

            damageableFixture.offsetFromBodyCenter.x = 0.3f * ConstVals.PPM * -facing.value
            shieldFixture.offsetFromBodyCenter.x = 0.2f * ConstVals.PPM * facing.value
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprites = OrderedMap<String, GameSprite>()
        val updateFunctions = ObjectMap<String, UpdateFunction<GameSprite>>()

        val heliSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        heliSprite.setSize(1.25f * ConstVals.PPM)
        sprites.put("heli", heliSprite)
        updateFunctions.put("heli") { _, _sprite ->
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.setCenter(body.getCenter())
        }

        for (i in 0 until FLAMES) {
            val flameSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
            flameSprite.setSize(0.5f * ConstVals.PPM)
            sprites.put("flame_$i", flameSprite)
            updateFunctions.put("flame_$i") { _, _sprite ->
                _sprite.hidden = !attacking
                _sprite.setPosition(
                    body.getBottomCenterPoint().add(
                        i * FLAME_PADDING * ConstVals.PPM * facing.value, -0.25f * ConstVals.PPM
                    ), if (isFacing(Facing.LEFT)) Position.BOTTOM_RIGHT else Position.BOTTOM_LEFT
                )
            }
        }

        return SpritesComponent(this, sprites, updateFunctions)
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animators = Array<Pair<() -> GameSprite, IAnimator>>()

        val heliAnimation = Animation(heliRegion!!, 1, 2, 0.1f, true)
        val heliAnimator = Animator(heliAnimation)
        animators.add({ sprites.get("heli") } to heliAnimator)

        for (i in 0 until FLAMES) {
            val flameAnimation = Animation(flameRegion!!, 1, 3, 0.1f, true)
            val flameAnimator = Animator(flameAnimation)
            animators.add({ sprites.get("flame_$i") } to flameAnimator)
        }

        return AnimationsComponent(this, animators)
    }
}